import { Outlet } from 'react-router'
import { useUiStore } from '@/shared/lib/ui-store'
import { SidebarInset, SidebarProvider } from '@/shared/ui/sidebar'
import { TooltipProvider } from '@/shared/ui/tooltip'
import { AppHeader } from './app-header'
import { AppSidebar } from './app-sidebar'

export function AppLayout() {
  const sidebarOpen = useUiStore((state) => state.sidebarOpen)
  const setSidebarOpen = useUiStore((state) => state.setSidebarOpen)

  return (
    <TooltipProvider>
      <SidebarProvider open={sidebarOpen} onOpenChange={setSidebarOpen}>
        <AppSidebar />
        <SidebarInset>
          <AppHeader />
          <main className="flex-1 p-6">
            <Outlet />
          </main>
        </SidebarInset>
      </SidebarProvider>
    </TooltipProvider>
  )
}
