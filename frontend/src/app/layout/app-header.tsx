import { Separator } from '@/shared/ui/separator'
import { SidebarTrigger } from '@/shared/ui/sidebar'

export function AppHeader() {
  return (
    <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
      <SidebarTrigger aria-label="Alternar barra lateral" />
      <Separator orientation="vertical" className="h-4" />
      <span className="text-sm text-muted-foreground">LeadCenter ERP</span>
    </header>
  )
}
