import { createContext } from 'react'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthState {
  status: AuthStatus
  /** Permission codes granted to the current user. Presentation only: the backend enforces. */
  permissions: readonly string[]
}

export const ANONYMOUS: AuthState = { status: 'anonymous', permissions: [] }

export const AuthContext = createContext<AuthState>(ANONYMOUS)
