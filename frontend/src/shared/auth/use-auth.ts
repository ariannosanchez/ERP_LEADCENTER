import { useContext } from 'react'
import { AuthContext } from './auth-context'

export function useAuth() {
  return useContext(AuthContext)
}

/** Presentation-only check; the backend remains the authority. */
export function useHasPermission(permission: string): boolean {
  return useAuth().permissions.includes(permission)
}
