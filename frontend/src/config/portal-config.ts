import { reactive } from 'vue'
import { http } from '../api/http'

export interface PortalConfig {
  organizationName: string
  servicePhone: string
  serviceHours: string
  emergencyPhone: string
}

export function normalizePortalConfig(value: Partial<PortalConfig> | null | undefined): PortalConfig {
  return {
    organizationName: value?.organizationName?.trim() || '社区卫生服务中心',
    servicePhone: value?.servicePhone?.trim() || '',
    serviceHours: value?.serviceHours?.trim() || '',
    emergencyPhone: value?.emergencyPhone?.trim() || '120',
  }
}

export const portalConfig = reactive<PortalConfig>(normalizePortalConfig(undefined))

export async function loadPortalConfig() {
  const { data } = await http.get<PortalConfig>('/public/portal-config')
  Object.assign(portalConfig, normalizePortalConfig(data))
  return portalConfig
}
