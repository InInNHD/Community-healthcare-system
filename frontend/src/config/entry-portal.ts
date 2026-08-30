export type EntryPortal = 'admin' | 'staff' | 'resident'

export function portalFromEntry(pathname: string, marker?: string | null): EntryPortal | null {
  const normalizedMarker = marker?.trim().toLowerCase()
  if (normalizedMarker === 'admin' || normalizedMarker === 'staff' || normalizedMarker === 'resident') {
    return normalizedMarker
  }
  const filename = pathname.split('/').pop()?.toLowerCase()
  if (filename === 'admin.html') return 'admin'
  if (filename === 'staff.html') return 'staff'
  if (filename === 'resident.html') return 'resident'
  return null
}

export function isPortalAllowed(entry: EntryPortal | null, portal: EntryPortal) {
  return entry === null || entry === portal
}

export function entryHome(portal: EntryPortal) {
  return `/${portal}`
}

export const configuredEntryPortal = portalFromEntry(
  globalThis.location?.pathname ?? '/',
  globalThis.document?.documentElement.dataset.portalEntry,
)
