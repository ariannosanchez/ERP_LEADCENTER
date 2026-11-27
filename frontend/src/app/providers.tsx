import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider } from 'react-router/dom'
import { AuthProvider, type AuthState } from '@/shared/auth'
import { createQueryClient } from './query-client'
import { router } from './router'

// Placeholder session until T8 wires the real current-user query into this provider.
// Permissions stay empty so no permission-gated UI is exposed by accident.
const PLACEHOLDER_AUTH: AuthState = { status: 'authenticated', permissions: [] }

export function AppProviders() {
  const [queryClient] = useState(createQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider value={PLACEHOLDER_AUTH}>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  )
}
