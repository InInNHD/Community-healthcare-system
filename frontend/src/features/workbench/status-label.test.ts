import { describe, expect, it } from 'vitest'
import { businessStatusLabel } from './status-label'

describe('business status labels', () => {
  it('renders core workflow states in plain Chinese', () => {
    expect(businessStatusLabel('WAITING')).toBe('候诊中')
    expect(businessStatusLabel('SIGNED')).toBe('已签署')
    expect(businessStatusLabel('DISPENSED')).toBe('已发药')
    expect(businessStatusLabel('CONSENTED')).toBe('居民已同意')
    expect(businessStatusLabel('DEAD')).toBe('待人工处理')
  })

  it('keeps unknown states readable instead of showing a blank label', () => {
    expect(businessStatusLabel('CUSTOM_STATE')).toBe('CUSTOM STATE')
    expect(businessStatusLabel(undefined)).toBe('状态未知')
  })
})
