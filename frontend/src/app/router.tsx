import { createBrowserRouter, type RouteObject } from 'react-router'
import { AppLayout } from './layout/app-layout'
import { HomePage } from './pages/home-page'
import { LoginPlaceholderPage } from './pages/login-placeholder-page'
import { NotFoundPage } from './pages/not-found-page'
import { ProtectedRoute } from './protected-route'

/** Feature routes are added here as slices land; guard them with `<ProtectedRoute permission=…>`. */
export const appRoutes: RouteObject[] = [
  { path: '/login', element: <LoginPlaceholderPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <HomePage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]

export const router = createBrowserRouter(appRoutes)
