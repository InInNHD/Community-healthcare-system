import { describe, expect, it } from 'vitest'
import { modulesForRoles } from './module-catalog'

describe('R2-R5 portal module visibility', () => {
  it('keeps resident services self-service only and exposes no clinical treatment entry', () => {
    const residentModules = modulesForRoles('resident', ['RESIDENT'])

    expect(residentModules.map(module => module.key)).toEqual([
      'appointment', 'billing', 'contract', 'health-programs', 'referral',
      'record-access', 'consultation', 'feedback',
    ])
    expect(residentModules.every(module => module.capability !== 'CLINICAL_WRITE')).toBe(true)
    expect(residentModules.map(module => module.label)).not.toContain('诊断与处方')
  })

  it('places pharmacist and registrar into distinct operational modules', () => {
    expect(modulesForRoles('staff', ['PHARMACIST']).map(module => module.key)).toEqual([
      'prescription-review', 'dispensing',
    ])
    expect(modulesForRoles('staff', ['REGISTRAR']).map(module => module.key)).toEqual([
      'checkin-queue', 'billing-counter',
    ])
  })

  it('shows clinical and public-health work to the appropriate care roles', () => {
    const doctorKeys = modulesForRoles('staff', ['DOCTOR']).map(module => module.key)
    const nurseKeys = modulesForRoles('staff', ['NURSE']).map(module => module.key)

    expect(doctorKeys).toContain('encounter')
    expect(doctorKeys).toContain('prescription')
    expect(doctorKeys).toContain('referral-management')
    expect(nurseKeys).toContain('public-health-followup')
    expect(nurseKeys).not.toContain('prescription')
  })
})
