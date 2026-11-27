import type { ReactNode } from 'react'
import { useHasPermission } from './use-auth'

interface CanProps {
  permission: string
  fallback?: ReactNode
  children: ReactNode
}

/** Renders children only when the current user holds the permission. */
export function Can({ permission, fallback = null, children }: CanProps) {
  return useHasPermission(permission) ? children : fallback
}
