import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth, useHasPermission } from '@/shared/auth'
import { Skeleton } from '@/shared/ui/skeleton'

interface ProtectedRouteProps {
  /** Optional permission required to see the nested routes (presentation only). */
  permission?: string
}

export function ProtectedRoute({ permission }: ProtectedRouteProps) {
  const { status } = useAuth()
  const location = useLocation()
  const allowed = useHasPermission(permission ?? '')

  if (status === 'loading') {
    return <Skeleton className="m-6 h-8 w-48" aria-label="Cargando" />
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (permission && !allowed) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
