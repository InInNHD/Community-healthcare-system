import { describe, expect, it } from 'vitest'
import { entryHome, isPortalAllowed, portalFromEntry } from './entry-portal'

describe('portal build entries', () => {
  it('maps every dedicated html entry to exactly one portal', () => {
    expect(portalFromEntry('/admin.html')).toBe('admin')
    expect(portalFromEntry('/staff.html')).toBe('staff')
    expect(portalFromEntry('/resident.html')).toBe('resident')
    expect(portalFromEntry('/index.html')).toBeNull()
  })

  it('rejects cross-portal navigation for dedicated entries', () => {
    expect(isPortalAllowed('admin', 'admin')).toBe(true)
    expect(isPortalAllowed('admin', 'staff')).toBe(false)
    expect(isPortalAllowed('resident', 'admin')).toBe(false)
    expect(entryHome('staff')).toBe('/staff')
  })
})
