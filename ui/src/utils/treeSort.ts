export interface SortableNode {
  name: string
  dtoFile?: {
    discovered_at?: string
  }
}

export function sortNodes<T extends SortableNode>(nodes: T[], orderBy: 'name' | 'date', orderDir: 'asc' | 'desc'): T[] {
  const sorted = [...nodes].sort((a, b) => {
    if (orderBy === 'date') {
      const aTime = a.dtoFile?.discovered_at ? new Date(a.dtoFile.discovered_at).getTime() : 0
      const bTime = b.dtoFile?.discovered_at ? new Date(b.dtoFile.discovered_at).getTime() : 0
      return aTime - bTime
    }
    return a.name.localeCompare(b.name)
  })
  return orderDir === 'asc' ? sorted : sorted.reverse()
}