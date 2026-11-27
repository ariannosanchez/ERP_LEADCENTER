import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/shared/api/http'

const MAX_RETRIES = 2

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Client errors (4xx) will not succeed on retry; only transient failures do.
        retry: (failureCount, error) =>
          !(error instanceof ApiError && error.status >= 400 && error.status < 500) &&
          failureCount < MAX_RETRIES,
      },
      mutations: { retry: false },
    },
  })
}
