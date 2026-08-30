import { describe, expect, it, vi } from 'vitest'
import {
  createPlatformApi,
  type PlatformTransport,
} from './platform'

function transportSpy() {
  const post = vi.fn(async (_url: string, _body?: unknown, _options?: unknown) => ({ data: {} }))
  const put = vi.fn(async (_url: string, _body?: unknown, _options?: unknown) => ({ data: {} }))
  const patch = vi.fn(async (_url: string, _body?: unknown, _options?: unknown) => ({ data: {} }))
  const del = vi.fn(async (_url: string) => ({ data: {} }))
  const get = vi.fn(async (_url: string) => ({ data: [] }))
  return { transport: { get, post, put, patch, delete: del } as PlatformTransport, get, post, put, patch, del }
}

describe('platform API identity boundary', () => {
  it('creates a resident appointment without accepting patientId or staffId', async () => {
    const { transport, post } = transportSpy()
    const api = createPlatformApi(transport)

    await api.createResidentAppointment({ slotId: 18, reason: '复诊开药' })

    expect(post.mock.calls[0]?.[0]).toBe('/v1/resident/scheduling/appointments')
    expect(post.mock.calls[0]?.[1]).toEqual({ slotId: 18, reason: '复诊开药' })
    expect(post.mock.calls[0]?.[2]).toEqual({ headers: { 'Idempotency-Key': expect.any(String) } })
    expect(JSON.stringify(post.mock.calls[0]?.[1])).not.toMatch(/patientId|staffId/)
  })

  it('submits resident-owned actions without subject identifiers', async () => {
    const { transport, post } = transportSpy()
    const api = createPlatformApi(transport)

    await api.confirmContract(5, { accepted: true })
    await api.leaveConsultation({ category: '健康咨询', content: '如何控制盐摄入？' })
    await api.submitFeedback({ serviceId: 23, rating: 5, comment: '服务耐心' })

    for (const call of post.mock.calls) {
      expect(JSON.stringify(call[1])).not.toMatch(/patientId|residentId|staffId/)
    }
  })

  it('derives staff identity from authentication for encounter and dispensing commands', async () => {
    const { transport, post, put } = transportSpy()
    const api = createPlatformApi(transport)

    expect(typeof api.startEncounter).toBe('function')
    post.mockImplementation(async (url: string) => ({
      data: url.endsWith('/start')
        ? { id: 7, version: 3, status: 'DRAFT' }
        : url.endsWith('/sign') ? { id: 7, version: 5, status: 'SIGNED' } : {},
    }))
    put.mockResolvedValue({ data: { id: 7, version: 4, status: 'DRAFT' } })

    await api.startEncounter(41)
    await api.saveEncounterDraft(7, { chiefComplaint: '头晕', diagnosisCodes: ['I10'] })
    await api.signEncounter(7)
    await api.createPrescription({ encounterId: 7, diagnosis: 'I10', items: [{ skuId: 3, quantity: 14, dosage: '每日一次' }] })
    await api.signPrescription(9)
    await api.dispensePrescription(9, { batchAllocations: [{ batchId: 2, quantity: 1 }] })

    expect(put).toHaveBeenCalledWith('/v1/staff/encounters/7/draft', {
      body: '头晕', version: 3,
    })
    expect(post).toHaveBeenCalledWith('/v1/staff/encounters/7/sign', { version: 4 })
    for (const call of [...post.mock.calls, ...put.mock.calls]) {
      expect(JSON.stringify(call[1])).not.toMatch(/patientId|doctorId|pharmacistId|staffId/)
    }
  })
})
