import { beforeEach, describe, expect, it } from 'vitest'
import { OpenAPI } from './generated'
import { configureGeneratedClient } from './generated-client'

describe('generated API client runtime configuration', () => {
  beforeEach(() => localStorage.clear())

  it('uses same-origin credentials without exposing a bearer token to JavaScript', async () => {
    configureGeneratedClient()

    expect(OpenAPI.BASE).toBe('')
    expect(OpenAPI.WITH_CREDENTIALS).toBe(true)
    expect(OpenAPI.CREDENTIALS).toBe('include')
    expect(OpenAPI.TOKEN).toBeUndefined()
    expect(localStorage.getItem('healthcare_token')).toBeNull()
  })
})
