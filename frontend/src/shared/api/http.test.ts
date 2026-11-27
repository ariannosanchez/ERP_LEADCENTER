import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, customFetch, http } from './http'

const fetchMock = vi.fn<typeof fetch>()

function lastRequest() {
  const [url, init] = fetchMock.mock.calls.at(-1)!
  return { url: String(url), init: init!, headers: new Headers(init!.headers) }
}

function jsonResponse(body: unknown, init: ResponseInit = {}, type = 'application/json') {
  return new Response(JSON.stringify(body), {
    status: 200,
    ...init,
    headers: { 'Content-Type': type, ...init.headers },
  })
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('http', () => {
  it('returns parsed JSON, prefixes /api and includes credentials', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 1 }))

    const result = await http<{ id: number }>('/v1/things')

    expect(result).toEqual({ id: 1 })
    const { url, init, headers } = lastRequest()
    expect(url).toBe('/api/v1/things')
    expect(init.credentials).toBe('include')
    expect(headers.get('Accept')).toBe('application/json')
  })

  it('does not prefix paths that already start with /api/', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}))
    await http('/api/v1/things')
    expect(lastRequest().url).toBe('/api/v1/things')
  })

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    await expect(http('/v1/things', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('parses a problem+json error into ApiError', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          type: 'about:blank',
          title: 'Not Found',
          status: 404,
          detail: 'User 7 not found',
          instance: '/api/v1/users/7',
        },
        { status: 404 },
        'application/problem+json',
      ),
    )

    const error = await http('/v1/users/7').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(404)
    expect(apiError.title).toBe('Not Found')
    expect(apiError.detail).toBe('User 7 not found')
    expect(apiError.type).toBe('about:blank')
    expect(apiError.errors).toEqual([])
    expect(apiError.message).toBe('User 7 not found')
  })

  it('exposes the validation errors list', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          title: 'Bad Request',
          status: 400,
          errors: [
            { field: 'email', message: 'must be a well-formed email address' },
            { field: 'name', message: 'must not be blank' },
          ],
        },
        { status: 400 },
        'application/problem+json',
      ),
    )

    const error = (await http('/v1/users', { method: 'POST', body: '{}' }).catch(
      (e: unknown) => e,
    )) as ApiError

    expect(error.status).toBe(400)
    expect(error.errors).toEqual([
      { field: 'email', message: 'must be a well-formed email address' },
      { field: 'name', message: 'must not be blank' },
    ])
    expect(error.fieldErrors()).toEqual({
      email: 'must be a well-formed email address',
      name: 'must not be blank',
    })
  })

  it('wraps a non-JSON error body into ApiError', async () => {
    fetchMock.mockResolvedValue(
      new Response('<html>Bad gateway</html>', {
        status: 502,
        statusText: 'Bad Gateway',
        headers: { 'Content-Type': 'text/html' },
      }),
    )

    const error = (await http('/v1/things').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(502)
    expect(error.title).toBe('Bad Gateway')
    expect(error.detail).toBeUndefined()
    expect(error.errors).toEqual([])
  })

  it('wraps a JSON error with a malformed body into ApiError', async () => {
    fetchMock.mockResolvedValue(
      new Response('not json', {
        status: 500,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    )

    const error = (await http('/v1/things').catch((e: unknown) => e)) as ApiError
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(500)
  })

  describe('CSRF header', () => {
    beforeEach(() => {
      document.cookie = 'XSRF-TOKEN=tok%3D123; path=/'
      fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))
    })

    it.each(['POST', 'PUT', 'PATCH', 'DELETE', 'post'])(
      'sends X-XSRF-TOKEN on %s',
      async (method) => {
        await http('/v1/things', { method })
        expect(lastRequest().headers.get('X-XSRF-TOKEN')).toBe('tok=123')
      },
    )

    it.each(['GET', 'HEAD', undefined])('does not send X-XSRF-TOKEN on %s', async (method) => {
      await http('/v1/things', { method })
      expect(lastRequest().headers.has('X-XSRF-TOKEN')).toBe(false)
    })

    it('omits the header when the cookie is absent', async () => {
      document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
      await http('/v1/things', { method: 'POST' })
      expect(lastRequest().headers.has('X-XSRF-TOKEN')).toBe(false)
    })
  })
})

describe('customFetch (orval mutator)', () => {
  it('wraps the response as { data, status, headers }', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }, { status: 201 }))

    const result = await customFetch<{ data: { ok: boolean }; status: number; headers: Headers }>(
      '/v1/things',
      { method: 'POST', body: '{}' },
    )

    expect(result.data).toEqual({ ok: true })
    expect(result.status).toBe(201)
    expect(result.headers).toBeInstanceOf(Headers)
  })

  it('throws ApiError on failure', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        { title: 'Forbidden', status: 403 },
        { status: 403 },
        'application/problem+json',
      ),
    )
    await expect(customFetch('/v1/things')).rejects.toBeInstanceOf(ApiError)
  })
})
