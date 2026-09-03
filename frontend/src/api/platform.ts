import { http } from './http'

type TransportResult<T> = Promise<{ data: T }>
interface RequestOptions { headers?: Record<string, string> }

export interface PlatformTransport {
  get<T = unknown>(url: string): TransportResult<T>
  post<T = unknown>(url: string, body?: unknown, options?: RequestOptions): TransportResult<T>
  put<T = unknown>(url: string, body?: unknown, options?: RequestOptions): TransportResult<T>
  patch<T = unknown>(url: string, body?: unknown, options?: RequestOptions): TransportResult<T>
  delete<T = unknown>(url: string): TransportResult<T>
}

export interface SlotView { id: number; startsAt: string; endsAt: string; departmentName: string; staffName: string; remaining: number }
export interface ScheduledAppointmentItem {
  id: number
  appointmentNo?: string
  patientId: number
  patientName?: string
  doctorId?: number
  doctorName?: string
  staffProfileId: number
  staffName: string
  department?: string
  departmentName?: string
  scheduledAt: string
  status: 'PENDING' | 'CONFIRMED' | 'CHECKED_IN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
  reason: string
}
export interface AppointmentCommand { slotId: number; reason: string }
export interface ContractConfirmation { accepted: boolean }
export interface ConsultationCommand { category: string; content: string }
export interface FeedbackCommand { serviceId: number; rating: number; comment?: string }
export interface EncounterDraftCommand { chiefComplaint: string; diagnosisCodes: string[] }
export interface EncounterView { id: number; version: number; status: string }
export interface PrescriptionCommand { encounterId: number; diagnosis: string; items: Array<{ skuId: number; quantity: number; dosage: string }> }
export interface DispensingCommand { batchAllocations: Array<{ batchId: number; quantity: number }> }
export interface QueueView { id: number; ticketNo: string; residentName: string; status: string; waitingMinutes: number }
export interface WorkItem { id: number; title: string; status: string; dueAt?: string }
export interface QualityMetric { code: string; label: string; value: number; unit: string; target?: number }

function idempotencyOptions(): RequestOptions {
  const randomKey = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return { headers: { 'Idempotency-Key': randomKey } }
}

export function createPlatformApi(transport: PlatformTransport = http) {
  const encounterVersions = new Map<number, number>()

  function rememberEncounter(encounter: EncounterView) {
    if (!Number.isInteger(encounter.id) || !Number.isInteger(encounter.version)) {
      throw new Error('接诊响应缺少有效的编号或版本，请刷新后重试')
    }
    encounterVersions.set(encounter.id, encounter.version)
  }

  function currentEncounterVersion(encounterId: number) {
    const version = encounterVersions.get(encounterId)
    if (version === undefined) throw new Error('请先从预约启动接诊，以获取当前病历版本')
    return version
  }

  return {
    listResidentSlots: () => transport.get<SlotView[]>('/v1/resident/scheduling/slots'),
    listResidentAppointments: () => transport.get<ScheduledAppointmentItem[]>('/v1/resident/scheduling/appointments'),
    createResidentAppointment: (command: AppointmentCommand) => transport.post('/v1/resident/scheduling/appointments', command, idempotencyOptions()),
    cancelResidentAppointment: (appointmentId: number) => transport.delete(`/v1/resident/scheduling/appointments/${appointmentId}`),
    listResidentInvoices: () => transport.get<WorkItem[]>('/v1/resident/billing/invoices'),
    listResidentContracts: () => transport.get<WorkItem[]>('/v1/resident/family-doctor/contracts'),
    confirmContract: (contractId: number, command: ContractConfirmation) => command.accepted
      ? transport.post(`/v1/resident/family-doctor/contracts/${contractId}/confirm`, {}, idempotencyOptions())
      : Promise.reject(new Error('当前合同只能确认；如有异议请通过健康咨询联系家庭医生团队')),
    listResidentVisits: () => transport.get<WorkItem[]>('/v1/resident/public-health/visits'),
    listResidentReferrals: () => transport.get<WorkItem[]>('/v1/resident/referrals'),
    consentReferral: (referralId: number) => transport.post(`/v1/resident/referrals/${referralId}/consent`, {}),
    requestRecordRelease: (command: { referralId?: number; scopeCode: string; purpose: string }) => transport.post('/v1/resident/records/releases', command),
    leaveConsultation: (command: ConsultationCommand) => transport.post('/v1/resident/messages', { subject: command.category, body: command.content }),
    listConsultations: () => transport.get<WorkItem[]>('/v1/resident/messages'),
    submitFeedback: (command: FeedbackCommand) => transport.post('/v1/resident/feedback', {
      businessType: 'COMMUNITY_SERVICE', businessId: String(command.serviceId), rating: command.rating, comments: command.comment,
    }),
    listStaffQueue: () => transport.get<QueueView[]>('/v1/staff/scheduling/queue'),
    listStaffAppointments: () => transport.get<ScheduledAppointmentItem[]>('/v1/staff/scheduling/appointments'),
    listStaffEncounters: () => transport.get<EncounterView[]>('/v1/staff/encounters'),
    checkInAppointment: (appointmentId: number) => transport.post(`/v1/staff/scheduling/appointments/${appointmentId}/check-in`, {}),
    startEncounter: async (appointmentId: number) => {
      const result = await transport.post<EncounterView>(`/v1/staff/scheduling/appointments/${appointmentId}/start`, {})
      rememberEncounter(result.data)
      return result
    },
    saveEncounterDraft: async (encounterId: number, command: EncounterDraftCommand) => {
      const result = await transport.put<EncounterView>(`/v1/staff/encounters/${encounterId}/draft`, {
        body: command.chiefComplaint,
        version: currentEncounterVersion(encounterId),
      })
      rememberEncounter(result.data)
      await Promise.all(command.diagnosisCodes.map(code => transport.post(`/v1/staff/encounters/${encounterId}/diagnoses`, { code, name: code, type: 'PRIMARY' })))
      return result
    },
    signEncounter: async (encounterId: number) => {
      const result = await transport.post<EncounterView>(`/v1/staff/encounters/${encounterId}/sign`, {
        version: currentEncounterVersion(encounterId),
      })
      rememberEncounter(result.data)
      return result
    },
    createPrescription: (command: PrescriptionCommand) => transport.post('/v1/staff/pharmacy/prescriptions', command),
    listPrescriptions: () => transport.get<WorkItem[]>('/v1/staff/pharmacy/prescriptions'),
    listMedicineSkus: () => transport.get<WorkItem[]>('/v1/staff/pharmacy/skus'),
    signPrescription: (prescriptionId: number) => transport.post(`/v1/staff/pharmacy/prescriptions/${prescriptionId}/sign`, {}, idempotencyOptions()),
    reviewPrescription: (prescriptionId: number, approved: boolean, comment: string) => transport.post(`/v1/staff/pharmacy/prescriptions/${prescriptionId}/review`, { approved, comment }, idempotencyOptions()),
    pickPrescription: (prescriptionId: number) => transport.post(`/v1/staff/pharmacy/prescriptions/${prescriptionId}/pick`, {}),
    checkPrescription: (prescriptionId: number) => transport.post(`/v1/staff/pharmacy/prescriptions/${prescriptionId}/check`, {}),
    dispensePrescription: (prescriptionId: number, _command: DispensingCommand) => transport.post(`/v1/staff/pharmacy/prescriptions/${prescriptionId}/dispense`, {}, idempotencyOptions()),
    listStaffInvoices: () => transport.get<WorkItem[]>('/v1/staff/billing/invoices'),
    issueInvoice: (invoiceId: number) => transport.post(`/v1/staff/billing/invoices/${invoiceId}/issue`, {}),
    listStaffTasks: () => transport.get<WorkItem[]>('/v1/staff/family-doctor/tasks'),
    completeStaffTask: (taskId: number, summary: string) => transport.post(`/v1/staff/family-doctor/tasks/${taskId}/complete`, { summary }, idempotencyOptions()),
    listStaffVisits: () => transport.get<WorkItem[]>('/v1/staff/public-health/visits'),
    verifyStaffVisit: (visitId: number) => transport.post(`/v1/staff/public-health/visits/${visitId}/verify`, {}, idempotencyOptions()),
    listStaffReferrals: () => transport.get<WorkItem[]>('/v1/staff/referrals'),
    submitStaffReferral: (referralId: number) => transport.post(`/v1/staff/referrals/${referralId}/submit`, {}, idempotencyOptions()),
    listStaffMessages: () => transport.get<WorkItem[]>('/v1/staff/messages'),
    replyToMessage: (messageId: number, body: string) => transport.post(`/v1/staff/messages/${messageId}/replies`, { body }),
    listOrganizations: () => transport.get<WorkItem[]>('/v1/admin/organizations'),
    listServicePackages: () => transport.get<WorkItem[]>('/v1/admin/family-doctor/catalog'),
    listIntegrations: () => transport.get<WorkItem[]>('/v1/admin/integrations/outbox'),
    retryIntegration: (eventId: number) => transport.post(`/v1/admin/integrations/outbox/${eventId}/retry`, {}),
    listQualityMetrics: () => transport.get<QualityMetric[]>('/v1/admin/quality/snapshots'),
    refreshQualityMetrics: () => transport.post('/v1/admin/quality/snapshots/refresh', {}),
    listRecordReleases: () => transport.get<WorkItem[]>('/v1/resident/records/releases'),
    listFeedback: () => transport.get<WorkItem[]>('/v1/resident/feedback'),
  }
}

export const platformApi = createPlatformApi()
