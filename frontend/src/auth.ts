import { reactive } from 'vue'
import type { LoginResponse as GeneratedLoginResponse } from './api/generated'
import { http } from './api/http'

interface LoginResponse extends Required<Pick<GeneratedLoginResponse,
  'username' | 'displayName' | 'roles' | 'portal' | 'mustChangePassword'>> {
  portal: Portal
  subjectId?: number
}

export type Portal = 'admin' | 'staff' | 'resident'
export type AuthUser = LoginResponse
export interface MfaChallenge { mfaRequired: true; challengeToken: string; expiresIn: number }
export type LoginOutcome = { authenticated: true } | { authenticated: false; challenge: MfaChallenge }

const saved = localStorage.getItem('healthcare_user')
export const auth = reactive({
  authenticated: Boolean(saved),
  user: saved ? JSON.parse(saved) as AuthUser : null,
})

function saveSession(response: GeneratedLoginResponse) {
  if (!response.username || !response.displayName || !response.roles
    || !response.portal || typeof response.mustChangePassword !== 'boolean') {
    throw new Error('登录响应不符合 OpenAPI 契约')
  }
  const data = response as LoginResponse
  auth.authenticated = true
  auth.user = {
    username: data.username,
    displayName: data.displayName,
    roles: data.roles,
    portal: data.portal,
    subjectId: data.subjectId,
    mustChangePassword: data.mustChangePassword,
  }
  localStorage.setItem('healthcare_user', JSON.stringify(auth.user))
}

async function establishBrowserSession(response: GeneratedLoginResponse) {
  saveSession(response)
  await http.get('/auth/csrf')
}

export async function login(username: string, password: string, portal: Portal): Promise<LoginOutcome> {
  const { data: response } = await http.post<GeneratedLoginResponse | MfaChallenge>('/auth/login', { username, password, portal })
  if ('mfaRequired' in response && response.mfaRequired) {
    if (!response.challengeToken) throw new Error('MFA 挑战响应不完整')
    return { authenticated: false, challenge: response }
  }
  await establishBrowserSession(response)
  return { authenticated: true }
}

export async function verifyMfa(challengeToken: string, code: string) {
  const { data } = await http.post<GeneratedLoginResponse>('/auth/mfa/verify', { challengeToken, code })
  await establishBrowserSession(data)
}

export function homeForCurrentUser() {
  if (auth.user?.mustChangePassword) return '/change-password'
  if (auth.user?.roles.includes('ADMIN')) return '/admin'
  if (auth.user?.roles.some(role => ['DOCTOR', 'NURSE', 'PHARMACIST', 'REGISTRAR'].includes(role))) return '/staff'
  if (auth.user?.roles.includes('RESIDENT')) return '/resident'
  return '/login'
}

export function hasAnyRole(roles: string[]) {
  return Boolean(auth.user?.roles.some(role => roles.includes(role)))
}

export async function logout() {
  if (auth.authenticated) await http.post('/auth/logout').catch(() => undefined)
  auth.authenticated = false
  auth.user = null
  localStorage.removeItem('healthcare_user')
}
