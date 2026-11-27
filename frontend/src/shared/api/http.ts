/**
 * Fetch wrapper for the same-origin `/api` backend.
 *
 * - Sends the session cookie (`credentials: 'include'`).
 * - Echoes the `XSRF-TOKEN` cookie as `X-XSRF-TOKEN` on mutating methods.
 * - Turns RFC 9457 Problem Details (`application/problem+json`) into a typed {@link ApiError}.
 */

const API_PREFIX = '/api'
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export interface FieldError {
  field: string
  message: string
}

interface ProblemBody {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail: string | undefined
  readonly type: string | undefined
  readonly instance: string | undefined
  readonly errors: readonly FieldError[]

  constructor(status: number, problem: ProblemBody = {}, fallbackTitle = '') {
    const title = problem.title ?? fallbackTitle
    super(problem.detail ?? title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.detail = problem.detail
    this.type = problem.type
    this.instance = problem.instance
    this.errors = Array.isArray(problem.errors) ? problem.errors : []
  }

  /** First message per field, convenient for mapping onto form fields. */
  fieldErrors(): Record<string, string> {
    const byField: Record<string, string> = {}
    for (const { field, message } of this.errors) {
      byField[field] ??= message
    }
    return byField
  }
}

function readCookie(name: string): string | undefined {
  for (const part of document.cookie.split(';')) {
    const separator = part.indexOf('=')
    if (separator === -1) continue
    if (part.slice(0, separator).trim() === name) {
      const value = part.slice(separator + 1).trim()
      return value ? decodeURIComponent(value) : undefined
    }
  }
  return undefined
}

function buildUrl(path: string): string {
  return path === API_PREFIX || path.startsWith(`${API_PREFIX}/`) ? path : `${API_PREFIX}${path}`
}

function isJson(response: Response): boolean {
  const type = response.headers.get('Content-Type') ?? ''
  return type.includes('json')
}

async function toApiError(response: Response): Promise<ApiError> {
  if (isJson(response)) {
    try {
      return new ApiError(
        response.status,
        (await response.json()) as ProblemBody,
        response.statusText,
      )
    } catch {
      // Malformed body: fall through to the status-only error.
    }
  }
  return new ApiError(response.status, {}, response.statusText)
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204 || response.status === 205) return undefined
  if (isJson(response)) return response.json()
  const text = await response.text()
  return text === '' ? undefined : text
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (!headers.has('Accept')) headers.set('Accept', 'application/json')

  if (MUTATING_METHODS.has(method)) {
    const token = readCookie(CSRF_COOKIE)
    if (token) headers.set(CSRF_HEADER, token)
  }

  const response = await fetch(buildUrl(path), {
    ...init,
    method,
    headers,
    credentials: 'include',
  })
  if (!response.ok) throw await toApiError(response)
  return response
}

/** Returns the parsed body (`undefined` for empty responses). Throws {@link ApiError} on failure. */
export async function http<T = unknown>(path: string, init?: RequestInit): Promise<T> {
  return (await parseBody(await request(path, init))) as T
}

/**
 * Custom mutator for the orval-generated client (fetch mode). Orval types `T` as the
 * `{ data, status, headers }` envelope of the operation.
 */
export async function customFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await request(path, init)
  return {
    data: await parseBody(response),
    status: response.status,
    headers: response.headers,
  } as T
}
