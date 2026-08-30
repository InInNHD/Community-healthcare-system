import { ApiError, OpenAPI } from './generated'

export function configureGeneratedClient() {
  OpenAPI.BASE = ''
  OpenAPI.WITH_CREDENTIALS = true
  OpenAPI.CREDENTIALS = 'include'
  OpenAPI.TOKEN = undefined
}

export function generatedApiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const body = error.body as { message?: string } | undefined
    return body?.message || error.message || '服务暂时不可用，请稍后重试'
  }
  return error instanceof Error ? error.message : '服务暂时不可用，请稍后重试'
}

configureGeneratedClient()
