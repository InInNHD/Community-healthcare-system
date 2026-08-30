import { describe, expect, it } from 'vitest'
import { normalizePortalConfig } from './portal-config'

describe('portal configuration', () => {
  it('uses safe non-clinical fallbacks without inventing a service number', () => {
    expect(normalizePortalConfig({ organizationName: '  健康路社区卫生服务中心  ' })).toEqual({
      organizationName: '健康路社区卫生服务中心',
      servicePhone: '',
      serviceHours: '',
      emergencyPhone: '120',
    })
  })
})
