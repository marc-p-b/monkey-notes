export interface DatedTranscript {
  documented_at?: string | null
  discovered_at?: string | null
  transcripted_at?: string | null
}

/**
 * The date a note is filed under. documented_at is the date written on the note itself (parsed from
 * the title by the OCR pipeline) and is what the user means by "when is this note from";
 * discovered_at (first sync) and transcripted_at (last OCR run) are only fallbacks for a note whose
 * title carried no date. Returns null for a transcript carrying none of the three.
 */
export function effectiveDate(transcript?: DatedTranscript | null): Date | null {
  const raw = transcript?.documented_at ?? transcript?.discovered_at ?? transcript?.transcripted_at
  if (!raw) return null
  const date = new Date(raw)
  return isNaN(date.getTime()) ? null : date
}

//counted in calendar days, not in elapsed milliseconds: a note written late yesterday evening is
//"1 day ago" this morning, not "0 days ago" because fewer than 24h have passed
function daysBetween(from: Date, to: Date) {
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  return Math.round((startOfDay(to) - startOfDay(from)) / 86400000)
}

/**
 * How old the note is, relative to today. A documented_at parsed from a title can land in the
 * future (a note dated ahead, or a misread date), so the negative side is handled too rather than
 * showing "-3 days ago".
 */
export function formatAge(date: Date | null): string {
  if (!date) return ''
  const days = daysBetween(date, new Date())
  if (days === 0) return 'Today'
  if (days === 1) return 'Yesterday'
  if (days < 0) return days === -1 ? 'Tomorrow' : `in ${-days} days`
  return `${days} days ago`
}

export function formatDayMonth(date: Date | null): string {
  if (!date) return ''
  return date.toLocaleDateString(undefined, { day: '2-digit', month: 'short' })
}
