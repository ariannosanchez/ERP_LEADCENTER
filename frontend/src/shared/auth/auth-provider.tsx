import type { ReactNode } from 'react'
import { AuthContext, type AuthState } from './auth-context'

interface AuthProviderProps {
  value: AuthState
  children: ReactNode
}

/** Feeds the auth context. The session/current-user wiring lives in the app layer (T8). */
export function AuthProvider({ value, children }: AuthProviderProps) {
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
