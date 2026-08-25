/**
 * Turns named entity markup (`<T:tag>`, `<P:person>`, `# heading`, …) into display HTML, using the
 * start/end offsets the backend already stored for each entity.
 *
 * Extracted verbatim from TranscriptPage.vue so the quicknote feed renders a `<T:invoice>` exactly
 * the way a scanned page does — previously every new verb would have had to be added in two places.
 */

export interface RenderableNamedEntity {
  uuid: string
  verb: string
  value: string
  start: number
  end: number
}

export interface RenderNamedEntitiesOptions {
  /**
   * Inline image for a `diagramNextPage` entity. Transcript pages pass the next page's object URL;
   * a quicknote never has one, so the entity renders as a plain reference.
   */
  diagramImageSrc?: string | null
}

export function replaceSubstring(str: string, start: number, end: number, replacement: string): string {
  return str.slice(0, start) + replacement + str.slice(end)
}

export function renderNamedEntities(
  source: string,
  entities: RenderableNamedEntity[] | null | undefined,
  options: RenderNamedEntitiesOptions = {}
): string {
  let out = source ?? ''

  // Offsets refer to the original string, so each replacement that changes the length shifts every
  // later one. lFix accumulates that drift. This assumes the entities arrive sorted by `start` and
  // don't overlap — which is how the backend produces them.
  let lFix = 0

  ;(entities ?? []).forEach(ne => {
    let repl = ''
    if (ne.verb == 'h2') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<h2 id='" + ne.uuid + "'>" + ne.value + "</h2>")
    } else if (ne.verb == 'h3') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<h3 id='" + ne.uuid + "'>" + ne.value + "</h3>")
    } else if (ne.verb == 'h4') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<h4 id='" + ne.uuid + "'>" + ne.value + "</h4>")
    } else if (ne.verb == 'h5') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<h5 id='" + ne.uuid + "'>" + ne.value + "</h5>")
    } else if (ne.verb == 'h6') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<h6 id='" + ne.uuid + "'>" + ne.value + "</h6>")
    } else if (ne.verb == 'tag') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-tag'></i> " + ne.value + "</span>")
    } else if (ne.verb == 'person') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-user'></i> " + ne.value + "</span>")
    } else if (ne.verb == 'email') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-envelope'> " + ne.value + "</i></span>")
    } else if (ne.verb == 'link') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-link'></i> " + ne.value + "</span>")
    } else if (ne.verb == 'dateUS' || ne.verb == 'dateEU' || ne.verb == 'dateISO') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-calendar'></i> " + ne.value + "</span>")
    } else if (ne.verb == 'checked') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<input id='" + ne.uuid + "' type='checkbox' checked /><label for='" + ne.uuid + "'>" + ne.value + "</label>")
    } else if (ne.verb == 'unchecked') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<input id='" + ne.uuid + "' type='checkbox' /><label for='" + ne.uuid + "'>" + ne.value + "</label>")
    } else if (ne.verb == 'diagram') {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-pen-to-square'></i> Diagram : " + ne.value + " </span>")
    } else if (ne.verb == 'diagramNextPage') {
      const inlineImg = options.diagramImageSrc
          ? "<br/><img src='" + options.diagramImageSrc + "' class='preview-img diagram-inline-img' alt='diagram' />"
          : ""
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "<span id='" + ne.uuid + "'><i class='pi pi-pen-to-square'></i> Diagram : " + ne.value + "</span>" + inlineImg)
    } else {
      repl = replaceSubstring(out, ne.start - lFix, ne.end - lFix, "|" + ne.verb + ":" + ne.value + "|")
    }
    lFix += out.length - repl.length
    out = repl
  })

  return out.replaceAll("\n", "<br/>")
}

/**
 * The distinct values of one verb across a set of entities, sorted — used to build the feed's
 * filter rail.
 */
export function distinctValues(entities: RenderableNamedEntity[], verb: string): string[] {
  const values = entities.filter(ne => ne.verb === verb).map(ne => ne.value)
  return [...new Set(values)].sort((a, b) => a.localeCompare(b))
}
