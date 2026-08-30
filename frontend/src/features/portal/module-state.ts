export type PortalModuleState = 'idle' | 'loading' | 'success' | 'empty' | 'error' | 'stale'

export function createModuleStates<K extends string>(keys: readonly K[]): Record<K, PortalModuleState> {
  return Object.fromEntries(keys.map(key => [key, 'idle'])) as Record<K, PortalModuleState>
}

export function markModulesLoading<K extends string>(
  states: Record<K, PortalModuleState>,
  keys: readonly K[],
): Record<K, PortalModuleState> {
  return { ...states, ...Object.fromEntries(keys.map(key => [key, 'loading'])) }
}

export function failedModuleNames(
  results: PromiseSettledResult<unknown>[],
  moduleNames: string[],
): string[] {
  return results.flatMap((result, index) => result.status === 'rejected' ? [moduleNames[index] ?? `模块 ${index + 1}`] : [])
}

export function resolveModuleState(
  result: PromiseSettledResult<unknown>,
  hasPreviousData: boolean,
  isEmpty: boolean,
): PortalModuleState {
  if (result.status === 'rejected') return hasPreviousData ? 'stale' : 'error'
  return isEmpty ? 'empty' : 'success'
}
