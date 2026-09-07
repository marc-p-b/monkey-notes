export interface SortableNode {
  name: string
  dtoFile?: {
    discovered_at?: string
  }
  //null for a folder — only a transcript row carries one (FileNode.transcriptDetails)
  transcriptDetails?: {
    transcript?: {
      title?: string
    }
  } | null
}

/**
 * What a row shows. A transcript is labelled with its title — the heading written on the note,
 * parsed out by the OCR pipeline — and falls back to the file name; the backend already applies
 * that fallback in DtoTranscript.fromEntity, so an empty title never reaches here. A folder has no
 * transcript and keeps its name.
 */
export function nodeLabel(node: SortableNode): string {
  return node.transcriptDetails?.transcript?.title || node.name
}

export function sortNodes<T extends SortableNode>(nodes: T[], orderBy: 'name' | 'date', orderDir: 'asc' | 'desc'): T[] {
  const sorted = [...nodes].sort((a, b) => {
    if (orderBy === 'date') {
      const aTime = a.dtoFile?.discovered_at ? new Date(a.dtoFile.discovered_at).getTime() : 0
      const bTime = b.dtoFile?.discovered_at ? new Date(b.dtoFile.discovered_at).getTime() : 0
      return aTime - bTime
    }
    //sorts on the displayed label, not on `name` — otherwise a list of titles reads unsorted
    return nodeLabel(a).localeCompare(nodeLabel(b))
  })
  return orderDir === 'asc' ? sorted : sorted.reverse()
}
