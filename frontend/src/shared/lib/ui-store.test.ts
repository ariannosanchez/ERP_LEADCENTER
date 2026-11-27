import { beforeEach, describe, expect, it } from 'vitest'
import { useUiStore } from './ui-store'

beforeEach(() => {
  useUiStore.setState(useUiStore.getInitialState())
})

describe('useUiStore', () => {
  it('starts with the sidebar open', () => {
    expect(useUiStore.getState().sidebarOpen).toBe(true)
  })

  it('toggles the sidebar', () => {
    useUiStore.getState().toggleSidebar()
    expect(useUiStore.getState().sidebarOpen).toBe(false)
    useUiStore.getState().toggleSidebar()
    expect(useUiStore.getState().sidebarOpen).toBe(true)
  })

  it('sets the sidebar explicitly', () => {
    useUiStore.getState().setSidebarOpen(false)
    expect(useUiStore.getState().sidebarOpen).toBe(false)
  })
})
