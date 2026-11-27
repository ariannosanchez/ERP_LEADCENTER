import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import { AuthProvider, type AuthState } from '@/shared/auth'
import { ProtectedRoute } from './protected-route'

function renderGuard(auth: AuthState, permission?: string) {
  const router = createMemoryRouter(
    [
      { path: '/login', element: <p>Login</p> },
      { path: '/', element: <p>Inicio</p> },
      {
        element: <ProtectedRoute permission={permission} />,
        children: [{ path: '/secret', element: <p>Secreto</p> }],
      },
    ],
    { initialEntries: ['/secret'] },
  )
  return render(
    <AuthProvider value={auth}>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

describe('ProtectedRoute', () => {
  it('renders nested routes for authenticated users', () => {
    renderGuard({ status: 'authenticated', permissions: [] })
    expect(screen.getByText('Secreto')).toBeInTheDocument()
  })

  it('redirects anonymous users to /login', () => {
    renderGuard({ status: 'anonymous', permissions: [] })
    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('shows a loading placeholder while the session is loading', () => {
    renderGuard({ status: 'loading', permissions: [] })
    expect(screen.getByLabelText('Cargando')).toBeInTheDocument()
  })

  it('redirects home when the required permission is missing', () => {
    renderGuard({ status: 'authenticated', permissions: [] }, 'usuarios.ver')
    expect(screen.getByText('Inicio')).toBeInTheDocument()
  })

  it('renders when the required permission is granted', () => {
    renderGuard({ status: 'authenticated', permissions: ['usuarios.ver'] }, 'usuarios.ver')
    expect(screen.getByText('Secreto')).toBeInTheDocument()
  })
})
