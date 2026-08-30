import { describe, expect, it } from 'vitest'
import { createModuleStates, failedModuleNames, markModulesLoading, resolveModuleState } from './module-state'

describe('portal module loading state', () => {
  it('reports every rejected module instead of treating partial loading as success', () => {
    const results: PromiseSettledResult<unknown>[] = [
      { status: 'fulfilled', value: [] },
      { status: 'rejected', reason: new Error('offline') },
    ]
    expect(failedModuleNames(results, ['预约', '健康记录'])).toEqual(['健康记录'])
  })

  it('distinguishes empty, stale, error and success states', () => {
    expect(resolveModuleState({ status: 'fulfilled', value: [] }, false, true)).toBe('empty')
    expect(resolveModuleState({ status: 'fulfilled', value: [1] }, false, false)).toBe('success')
    expect(resolveModuleState({ status: 'rejected', reason: 'offline' }, true, false)).toBe('stale')
    expect(resolveModuleState({ status: 'rejected', reason: 'offline' }, false, true)).toBe('error')
  })

  it('starts modules as idle and explicitly moves requested modules to loading', () => {
    const states = createModuleStates(['overview', 'appointments'] as const)

    expect(states).toEqual({ overview: 'idle', appointments: 'idle' })
    expect(markModulesLoading(states, ['appointments'])).toEqual({
      overview: 'idle',
      appointments: 'loading',
    })
  })
})
