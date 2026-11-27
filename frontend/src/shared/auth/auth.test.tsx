import { render, renderHook, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from './auth-provider'
import { Can } from './can'
import { useHasPermission } from './use-auth'

function wrapperFor(permissions: readonly string[]) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <AuthProvider value={{ status: 'authenticated', permissions }}>{children}</AuthProvider>
  }
}

describe('useHasPermission', () => {
  it('is true when the permission is granted', () => {
    const { result } = renderHook(() => useHasPermission('leads.ver'), {
      wrapper: wrapperFor(['leads.ver', 'leads.asignar']),
    })
    expect(result.current).toBe(true)
  })

  it('is false when the permission is missing', () => {
    const { result } = renderHook(() => useHasPermission('usuarios.crear'), {
      wrapper: wrapperFor(['leads.ver']),
    })
    expect(result.current).toBe(false)
  })

  it('is false without a provider (anonymous default)', () => {
    const { result } = renderHook(() => useHasPermission('leads.ver'))
    expect(result.current).toBe(false)
  })
})

describe('<Can>', () => {
  it('renders children when the permission is granted', () => {
    render(
      <Can permission="leads.ver">
        <p>Visible</p>
      </Can>,
      { wrapper: wrapperFor(['leads.ver']) },
    )
    expect(screen.getByText('Visible')).toBeInTheDocument()
  })

  it('renders nothing when the permission is missing', () => {
    render(
      <Can permission="leads.ver">
        <p>Visible</p>
      </Can>,
      { wrapper: wrapperFor([]) },
    )
    expect(screen.queryByText('Visible')).not.toBeInTheDocument()
  })

  it('renders the fallback when the permission is missing', () => {
    render(
      <Can permission="leads.ver" fallback={<p>Sin acceso</p>}>
        <p>Visible</p>
      </Can>,
      { wrapper: wrapperFor([]) },
    )
    expect(screen.getByText('Sin acceso')).toBeInTheDocument()
    expect(screen.queryByText('Visible')).not.toBeInTheDocument()
  })
})
