import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import { AuthProvider, type AuthState } from '@/shared/auth'
import { appRoutes } from './router'

function renderAt(path: string, auth: AuthState) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  return render(
    <AuthProvider value={auth}>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

describe('app routes', () => {
  it('renders the Spanish placeholder home inside the shell when authenticated', () => {
    renderAt('/', { status: 'authenticated', permissions: [] })
    expect(screen.getByRole('heading', { name: 'Bienvenido a LeadCenter ERP' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Inicio' })).toBeInTheDocument()
  })

  it('redirects anonymous users to /login', () => {
    renderAt('/', { status: 'anonymous', permissions: [] })
    expect(screen.getByText(/inicio de sesión/i)).toBeInTheDocument()
  })

  it('renders a not-found page inside the shell for unknown paths', () => {
    renderAt('/nope', { status: 'authenticated', permissions: [] })
    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })
})
