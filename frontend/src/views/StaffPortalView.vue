<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Box,
  Calendar,
  Checked,
  CircleCheck,
  Clock,
  DataAnalysis,
  DocumentAdd,
  FirstAidKit,
  Location,
  Memo,
  Phone,
  Plus,
  Refresh,
  Search,
  TrendCharts,
  User,
  View,
  Warning,
} from '@element-plus/icons-vue'
import { http } from '../api/http'
import {
  createModuleStates,
  failedModuleNames,
  markModulesLoading,
  resolveModuleState,
  type PortalModuleState,
} from '../features/portal/module-state'

type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
type PortalSection = 'overview' | 'appointments' | 'patients' | 'health' | 'chronic' | 'inventory'

interface StaffSummary {
  appointmentsToday: number
  pendingAppointments: number
  completedToday: number
  managedPatients: number
  chronicCases: number
  lowStockMedicines: number
}

interface StaffSummaryPayload extends Partial<StaffSummary> {
  patients?: number
}

interface StaffAppointment {
  id: number
  appointmentNo?: string
  patientId: number
  patientName?: string
  patientGender?: string
  patientAge?: number
  patientPhone?: string
  scheduledAt: string
  status: AppointmentStatus
  reason?: string
  remark?: string
}

interface StaffPatient {
  id: number
  name: string
  gender?: string
  birthDate?: string
  age?: number
  phone?: string
  idCard?: string
  address?: string
  chronicDiseaseCount?: number
  lastHealthRecordAt?: string
}

interface HealthRecord {
  id?: number
  patientId: number
  patientName?: string
  recordedAt: string
  heartRate?: number
  systolicPressure?: number
  diastolicPressure?: number
  bloodOxygen?: number
  weight?: number
  note?: string
}

interface ChronicCase {
  id: number
  patientId: number
  patientName?: string
  diseaseType: string
  riskLevel: string
  diagnosisDate?: string
  doctorId?: number
  managementPlan?: string
  lastFollowUpAt?: string
  nextFollowUpAt?: string
}

interface MedicineAlert {
  id: number
  name: string
  category?: string
  specification?: string
  stock: number
  minimumStock: number
}

interface ListPayload<T> {
  items?: T[]
  content?: T[]
  total?: number
}

interface HealthForm {
  patientId?: number
  recordedAt: string
  heartRate?: number
  systolicPressure?: number
  diastolicPressure?: number
  bloodOxygen?: number
  weight?: number
  note: string
}

const emptySummary: StaffSummary = {
  appointmentsToday: 0,
  pendingAppointments: 0,
  completedToday: 0,
  managedPatients: 0,
  chronicCases: 0,
  lowStockMedicines: 0,
}

// 医护门户接口集中在这里，后端契约调整时只需修改这一处。
const staffApi = {
  async summary() {
    const { data } = await http.get<StaffSummaryPayload>('/staff/summary')
    return data
  },
  async appointments() {
    const { data } = await http.get<StaffAppointment[] | ListPayload<StaffAppointment>>('/staff/appointments', {
      params: { today: true, size: 100 },
    })
    return data
  },
  async updateAppointmentStatus(id: number, status: AppointmentStatus) {
    const { data } = await http.patch<StaffAppointment>(`/staff/appointments/${id}/status`, { status })
    return data
  },
  async patients(keyword = '') {
    const { data } = await http.get<StaffPatient[] | ListPayload<StaffPatient>>('/staff/patients', {
      params: { keyword: keyword.trim(), size: 100 },
    })
    return data
  },
  async healthRecords(patientId?: number) {
    const { data } = await http.get<HealthRecord[] | ListPayload<HealthRecord>>('/staff/health-records', {
      params: patientId ? { patientId, size: 100 } : { size: 100 },
    })
    return data
  },
  async createHealthRecord(payload: HealthForm) {
    const { data } = await http.post<HealthRecord>('/staff/health-records', payload)
    return data
  },
  async chronicCases(keyword = '') {
    const { data } = await http.get<ChronicCase[] | ListPayload<ChronicCase>>('/staff/chronic-cases', {
      params: { keyword: keyword.trim(), size: 100 },
    })
    return data
  },
  async medicineAlerts() {
    const { data } = await http.get<MedicineAlert[] | ListPayload<MedicineAlert>>('/staff/medicine-alerts')
    return data
  },
}

function toItems<T>(payload: T[] | ListPayload<T>): T[] {
  if (Array.isArray(payload)) return payload
  return payload.items ?? payload.content ?? []
}

const activeSection = ref<PortalSection>('overview')
const staffModuleKeys = ['overview', 'appointments', 'patients', 'health', 'chronic', 'inventory'] as const
const loading = ref(false)
const portalLoadErrors = ref<string[]>([])
const moduleStates = reactive<Record<PortalSection, PortalModuleState>>(createModuleStates(staffModuleKeys))
const appointmentsLoading = ref(false)
const patientsLoading = ref(false)
const healthLoading = ref(false)
const chronicLoading = ref(false)
const inventoryLoading = ref(false)
const savingHealth = ref(false)
const healthDialogVisible = ref(false)
const patientDrawerVisible = ref(false)

const summary = reactive<StaffSummary>({ ...emptySummary })
const appointments = ref<StaffAppointment[]>([])
const patients = ref<StaffPatient[]>([])
const healthRecords = ref<HealthRecord[]>([])
const patientHealthRecords = ref<HealthRecord[]>([])
const chronicCases = ref<ChronicCase[]>([])
const medicineAlerts = ref<MedicineAlert[]>([])
const selectedPatient = ref<StaffPatient | null>(null)
const patientKeyword = ref('')
const chronicKeyword = ref('')
const appointmentFilter = ref<'ALL' | AppointmentStatus>('ALL')
const healthFormRef = ref()
const activeModuleState = computed(() => moduleStates[activeSection.value])
const activeModuleIssue = computed(() => {
  if (activeModuleState.value === 'error') return '当前模块尚未成功加载，请稍后重试。'
  if (activeModuleState.value === 'stale') return '当前显示上次成功加载的数据，本次刷新失败。'
  return ''
})

const now = new Date()
const healthForm = reactive<HealthForm>({
  patientId: undefined,
  recordedAt: toLocalDateTime(now),
  heartRate: undefined,
  systolicPressure: undefined,
  diastolicPressure: undefined,
  bloodOxygen: undefined,
  weight: undefined,
  note: '',
})

const healthRules = {
  patientId: [{ required: true, message: '请选择居民', trigger: 'change' }],
  recordedAt: [{ required: true, message: '请选择测量时间', trigger: 'change' }],
}

const navigation: Array<{ key: PortalSection; label: string; icon: unknown }> = [
  { key: 'overview', label: '工作概览', icon: DataAnalysis },
  { key: 'appointments', label: '今日预约', icon: Calendar },
  { key: 'patients', label: '居民检索', icon: User },
  { key: 'health', label: '健康登记', icon: TrendCharts },
  { key: 'chronic', label: '慢病管理', icon: Memo },
  { key: 'inventory', label: '库存预警', icon: Box },
]

const statusMeta: Record<AppointmentStatus, { text: string; type: 'warning' | 'primary' | 'success' | 'info' }> = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'primary' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
}

const filteredAppointments = computed(() => appointmentFilter.value === 'ALL'
  ? appointments.value
  : appointments.value.filter(item => item.status === appointmentFilter.value))

const pendingAppointments = computed(() => appointments.value.filter(item => item.status === 'PENDING'))
const nextAppointments = computed(() => appointments.value
  .filter(item => item.status === 'PENDING' || item.status === 'CONFIRMED')
  .slice()
  .sort((left, right) => new Date(left.scheduledAt).getTime() - new Date(right.scheduledAt).getTime())
  .slice(0, 5))
const recentHealthRecords = computed(() => healthRecords.value.slice(0, 6))
const urgentMedicines = computed(() => medicineAlerts.value
  .slice()
  .sort((left, right) => stockRatio(left) - stockRatio(right))
  .slice(0, 5))
const highRiskChronicCases = computed(() => chronicCases.value.filter(item => isHighRisk(item.riskLevel)))
const selectedPatientRecords = computed(() => selectedPatient.value
  ? patientHealthRecords.value.filter(item => item.patientId === selectedPatient.value?.id)
  : [])
const selectedPatientChronicCases = computed(() => selectedPatient.value
  ? chronicCases.value.filter(item => item.patientId === selectedPatient.value?.id)
  : [])

const summaryCards = computed(() => [
  { label: '今日预约', value: summary.appointmentsToday, note: `${summary.pendingAppointments} 项待确认`, icon: Calendar, color: '#146b63', tone: '#e5f4f0' },
  { label: '已完成接诊', value: summary.completedToday, note: '今日服务进度', icon: CircleCheck, color: '#35745a', tone: '#e7f5ec' },
  { label: '重点慢病居民', value: summary.chronicCases, note: `${highRiskChronicCases.value.length} 人高风险`, icon: FirstAidKit, color: '#9b5c25', tone: '#fff0df' },
  { label: '库存预警', value: summary.lowStockMedicines, note: '请及时反馈药房', icon: Bell, color: '#a9474d', tone: '#fdebed' },
])

function toLocalDateTime(value: Date): string {
  const offset = value.getTimezoneOffset() * 60_000
  return new Date(value.getTime() - offset).toISOString().slice(0, 19)
}

function formatDateTime(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date)
}

function formatDate(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN').format(date)
}

function timeOnly(value?: string): string {
  if (!value) return '--:--'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value.slice(11, 16) : new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date)
}

function maskIdCard(value?: string): string {
  if (!value || value.length < 8) return value || '—'
  return `${value.slice(0, 4)}**********${value.slice(-4)}`
}

function patientAge(patient: StaffPatient): string {
  if (patient.age != null) return `${patient.age} 岁`
  if (!patient.birthDate) return '年龄未知'
  const birth = new Date(patient.birthDate)
  if (Number.isNaN(birth.getTime())) return '年龄未知'
  const age = new Date().getFullYear() - birth.getFullYear()
  return `${Math.max(0, age)} 岁`
}

function stockRatio(item: MedicineAlert): number {
  return item.minimumStock > 0 ? item.stock / item.minimumStock : 1
}

function stockPercent(item: MedicineAlert): number {
  return Math.max(4, Math.min(100, Math.round(stockRatio(item) * 100)))
}

function isHighRisk(value?: string): boolean {
  const normalized = value?.toUpperCase() ?? ''
  return normalized === 'HIGH' || value === '高风险' || value === '高'
}

function riskText(value?: string): string {
  const labels: Record<string, string> = { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }
  return labels[value?.toUpperCase() ?? ''] ?? value ?? '未分级'
}

function riskType(value?: string): 'danger' | 'warning' | 'success' | 'info' {
  if (isHighRisk(value)) return 'danger'
  if (value?.toUpperCase() === 'MEDIUM' || value === '中风险' || value === '中') return 'warning'
  if (value?.toUpperCase() === 'LOW' || value === '低风险' || value === '低') return 'success'
  return 'info'
}

function patientName(patientId: number, explicitName?: string): string {
  return explicitName || patients.value.find(item => item.id === patientId)?.name || `居民 #${patientId}`
}

async function loadSummary() {
  const data = await staffApi.summary()
  Object.assign(summary, emptySummary, data, {
    managedPatients: data.managedPatients ?? data.patients ?? 0,
  })
}

async function loadAppointments() {
  appointmentsLoading.value = true
  try {
    appointments.value = toItems(await staffApi.appointments())
    summary.completedToday = appointments.value.filter(item => item.status === 'COMPLETED').length
    if (!summary.appointmentsToday) summary.appointmentsToday = appointments.value.length
  } finally {
    appointmentsLoading.value = false
  }
}

async function loadPatients() {
  patientsLoading.value = true
  try {
    patients.value = toItems(await staffApi.patients(patientKeyword.value))
  } finally {
    patientsLoading.value = false
  }
}

async function loadHealthRecords(patientId?: number) {
  healthLoading.value = true
  try {
    healthRecords.value = toItems(await staffApi.healthRecords(patientId))
  } finally {
    healthLoading.value = false
  }
}

async function loadPatientHealthRecords(patientId: number) {
  patientHealthRecords.value = toItems(await staffApi.healthRecords(patientId))
}

async function loadChronicCases() {
  chronicLoading.value = true
  try {
    chronicCases.value = toItems(await staffApi.chronicCases(chronicKeyword.value))
  } finally {
    chronicLoading.value = false
  }
}

async function loadMedicineAlerts() {
  inventoryLoading.value = true
  try {
    medicineAlerts.value = toItems(await staffApi.medicineAlerts())
  } finally {
    inventoryLoading.value = false
  }
}

async function loadPortal() {
  loading.value = true
  const hadPreviousData: Record<PortalSection, boolean> = Object.fromEntries(staffModuleKeys.map(key => [
    key,
    moduleStates[key] === 'success' || moduleStates[key] === 'empty' || moduleStates[key] === 'stale',
  ])) as Record<PortalSection, boolean>
  Object.assign(moduleStates, markModulesLoading(moduleStates, staffModuleKeys))
  try {
    const results = await Promise.allSettled([
      loadSummary(),
      loadAppointments(),
      loadPatients(),
      loadHealthRecords(),
      loadChronicCases(),
      loadMedicineAlerts(),
    ])
    portalLoadErrors.value = failedModuleNames(results, ['工作概览', '预约', '居民档案', '健康记录', '慢病管理', '库存预警'])
    const isEmpty: Record<PortalSection, boolean> = {
      overview: false,
      appointments: appointments.value.length === 0,
      patients: patients.value.length === 0,
      health: healthRecords.value.length === 0,
      chronic: chronicCases.value.length === 0,
      inventory: medicineAlerts.value.length === 0,
    }
    staffModuleKeys.forEach((key, index) => {
      moduleStates[key] = resolveModuleState(results[index], hadPreviousData[key], isEmpty[key])
    })
  } finally {
    loading.value = false
  }
}

async function refreshCurrentSection() {
  const actions: Record<PortalSection, () => Promise<unknown>> = {
    overview: loadPortal,
    appointments: loadAppointments,
    patients: loadPatients,
    health: () => loadHealthRecords(),
    chronic: loadChronicCases,
    inventory: loadMedicineAlerts,
  }
  const moduleNames: Record<PortalSection, string> = {
    overview: '工作概览', appointments: '预约', patients: '居民档案',
    health: '健康记录', chronic: '慢病管理', inventory: '库存预警',
  }
  const moduleName = moduleNames[activeSection.value]
  const key = activeSection.value
  const hadPreviousData = moduleStates[key] === 'success' || moduleStates[key] === 'empty' || moduleStates[key] === 'stale'
  moduleStates[key] = 'loading'
  try {
    await actions[activeSection.value]()
    if (activeSection.value === 'overview' && portalLoadErrors.value.length > 0) {
      ElMessage.warning(`部分数据更新失败：${portalLoadErrors.value.join('、')}`)
      return
    }
    portalLoadErrors.value = portalLoadErrors.value.filter(item => item !== moduleName)
    const isEmpty = key === 'appointments' ? appointments.value.length === 0
      : key === 'patients' ? patients.value.length === 0
        : key === 'health' ? healthRecords.value.length === 0
          : key === 'chronic' ? chronicCases.value.length === 0
            : key === 'inventory' ? medicineAlerts.value.length === 0
              : false
    moduleStates[key] = isEmpty ? 'empty' : 'success'
    ElMessage.success(`${moduleName}已更新`)
  } catch {
    moduleStates[key] = hadPreviousData ? 'stale' : 'error'
    if (!portalLoadErrors.value.includes(moduleName)) portalLoadErrors.value.push(moduleName)
  }
}

async function changeAppointmentStatus(item: StaffAppointment, status: AppointmentStatus) {
  const original = item.status
  item.status = status
  try {
    const updated = await staffApi.updateAppointmentStatus(item.id, status)
    Object.assign(item, updated)
    ElMessage.success(status === 'CONFIRMED' ? '预约已确认' : status === 'COMPLETED' ? '已完成本次接诊' : '预约状态已更新')
    await loadSummary()
  } catch {
    item.status = original
  }
}

async function openPatient(patient: StaffPatient) {
  selectedPatient.value = patient
  patientDrawerVisible.value = true
  patientHealthRecords.value = []
  await Promise.allSettled([loadPatientHealthRecords(patient.id), loadChronicCases()])
}

function openHealthDialog(patient?: StaffPatient) {
  Object.assign(healthForm, {
    patientId: patient?.id,
    recordedAt: toLocalDateTime(new Date()),
    heartRate: undefined,
    systolicPressure: undefined,
    diastolicPressure: undefined,
    bloodOxygen: undefined,
    weight: undefined,
    note: '',
  })
  healthDialogVisible.value = true
}

async function saveHealthRecord() {
  const valid = await healthFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingHealth.value = true
  try {
    await staffApi.createHealthRecord({ ...healthForm })
    ElMessage.success('健康数据已登记')
    healthDialogVisible.value = false
    await Promise.allSettled([
      loadHealthRecords(),
      selectedPatient.value ? loadPatientHealthRecords(selectedPatient.value.id) : Promise.resolve(),
      loadSummary(),
    ])
  } finally {
    savingHealth.value = false
  }
}

function goTo(section: PortalSection) {
  activeSection.value = section
}

onMounted(loadPortal)
</script>

<template>
  <div class="staff-portal" v-loading="loading">
    <section class="portal-hero">
      <div>
        <span class="hero-kicker"><i /> 医护工作门户</span>
        <h2>今天也一起守护社区健康</h2>
        <p>{{ new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(now) }} · 今日共有 {{ summary.appointmentsToday }} 项预约服务</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" plain @click="refreshCurrentSection">刷新数据</el-button>
        <el-button type="primary" :icon="DocumentAdd" @click="openHealthDialog()">登记健康数据</el-button>
      </div>
    </section>

    <el-alert
      v-if="portalLoadErrors.length"
      type="warning"
      :closable="false"
      show-icon
      :title="`部分模块加载失败：${portalLoadErrors.join('、')}`"
      description="已加载内容仍可使用；失败模块可能为空或保留上次成功数据，请稍后重试。"
    />
    <el-alert
      v-if="activeModuleIssue"
      class="module-state-alert"
      :type="activeModuleState === 'error' ? 'error' : 'warning'"
      :closable="false"
      show-icon
      :title="activeModuleIssue"
    />

    <nav class="portal-navigation surface" aria-label="医护门户功能导航">
      <button
        v-for="item in navigation"
        :key="item.key"
        type="button"
        :class="{ active: activeSection === item.key }"
        @click="goTo(item.key)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <b v-if="item.key === 'appointments' && summary.pendingAppointments">{{ summary.pendingAppointments }}</b>
        <b v-if="item.key === 'inventory' && summary.lowStockMedicines" class="danger">{{ summary.lowStockMedicines }}</b>
      </button>
    </nav>

    <template v-if="activeSection === 'overview'">
      <div class="summary-grid">
        <article v-for="card in summaryCards" :key="card.label" class="summary-card surface">
          <span class="summary-icon" :style="{ color: card.color, background: card.tone }">
            <el-icon><component :is="card.icon" /></el-icon>
          </span>
          <div><small>{{ card.label }}</small><strong>{{ card.value }}</strong><p>{{ card.note }}</p></div>
        </article>
      </div>

      <div class="overview-grid">
        <section class="panel surface">
          <header class="panel-header">
            <div><h3>接下来要做</h3><p>按预约时间排列的今日服务</p></div>
            <el-button link type="primary" @click="goTo('appointments')">查看全部</el-button>
          </header>
          <div v-if="nextAppointments.length" class="timeline-list">
            <article v-for="item in nextAppointments" :key="item.id" class="timeline-item">
              <div class="appointment-time"><b>{{ timeOnly(item.scheduledAt) }}</b><span>{{ item.status === 'PENDING' ? '待确认' : '候诊中' }}</span></div>
              <span class="timeline-dot" :class="item.status.toLowerCase()" />
              <div class="appointment-main">
                <div><strong>{{ patientName(item.patientId, item.patientName) }}</strong><span>{{ item.reason || '社区健康服务' }}</span></div>
                <el-tag :type="statusMeta[item.status].type" effect="light" round>{{ statusMeta[item.status].text }}</el-tag>
              </div>
            </article>
          </div>
          <el-empty v-else description="今日暂无待办预约" :image-size="82" />
        </section>

        <section class="panel surface">
          <header class="panel-header">
            <div><h3>业务提醒</h3><p>需要重点关注的服务事项</p></div>
          </header>
          <div class="reminder-stack">
            <button type="button" class="reminder pending" @click="goTo('appointments')">
              <span><el-icon><Clock /></el-icon></span>
              <div><strong>{{ pendingAppointments.length }} 项预约待确认</strong><p>请尽快联系居民并确认到诊时间</p></div>
              <el-icon><View /></el-icon>
            </button>
            <button type="button" class="reminder risk" @click="goTo('chronic')">
              <span><el-icon><Warning /></el-icon></span>
              <div><strong>{{ highRiskChronicCases.length }} 位高风险慢病居民</strong><p>建议优先安排健康随访</p></div>
              <el-icon><View /></el-icon>
            </button>
            <button type="button" class="reminder stock" @click="goTo('inventory')">
              <span><el-icon><Box /></el-icon></span>
              <div><strong>{{ medicineAlerts.length }} 种药品库存不足</strong><p>请核查并向药房反馈补货</p></div>
              <el-icon><View /></el-icon>
            </button>
          </div>
        </section>
      </div>

      <section class="panel surface quick-patients">
        <header class="panel-header">
          <div><h3>居民快速入口</h3><p>最近居民档案，可直接登记健康数据</p></div>
          <el-button link type="primary" @click="goTo('patients')">检索居民</el-button>
        </header>
        <div class="patient-card-grid">
          <article v-for="patient in patients.slice(0, 4)" :key="patient.id" class="mini-patient-card">
            <span class="patient-avatar">{{ patient.name?.slice(0, 1) }}</span>
            <div class="mini-patient-info"><strong>{{ patient.name }}</strong><p>{{ patient.gender || '性别未知' }} · {{ patientAge(patient) }}</p></div>
            <el-button size="small" plain @click="openPatient(patient)">查看档案</el-button>
            <el-button size="small" type="primary" plain @click="openHealthDialog(patient)">登记</el-button>
          </article>
        </div>
      </section>
    </template>

    <template v-else-if="activeSection === 'appointments'">
      <section class="panel surface section-panel">
        <header class="section-toolbar">
          <div><h3>今日预约</h3><p>确认到诊、完成接诊并持续跟踪服务状态</p></div>
          <el-radio-group v-model="appointmentFilter" size="large">
            <el-radio-button value="ALL">全部 {{ appointments.length }}</el-radio-button>
            <el-radio-button value="PENDING">待确认 {{ pendingAppointments.length }}</el-radio-button>
            <el-radio-button value="CONFIRMED">已确认</el-radio-button>
            <el-radio-button value="COMPLETED">已完成</el-radio-button>
          </el-radio-group>
        </header>
        <el-table v-loading="appointmentsLoading" :data="filteredAppointments" class="portal-table" :empty-text="moduleStates.appointments === 'error' ? '预约数据加载失败，请重试' : '今日暂无预约'">
          <el-table-column label="时间" width="104">
            <template #default="scope"><div class="table-time"><b>{{ timeOnly(scope.row.scheduledAt) }}</b><span>今日</span></div></template>
          </el-table-column>
          <el-table-column label="居民" min-width="150">
            <template #default="scope"><div class="person-cell"><span>{{ patientName(scope.row.patientId, scope.row.patientName).slice(0, 1) }}</span><div><b>{{ patientName(scope.row.patientId, scope.row.patientName) }}</b><small>{{ scope.row.patientGender || '居民' }} {{ scope.row.patientAge ? `· ${scope.row.patientAge}岁` : '' }}</small></div></div></template>
          </el-table-column>
          <el-table-column prop="appointmentNo" label="预约编号" min-width="145" show-overflow-tooltip />
          <el-table-column prop="reason" label="服务事由" min-width="190" show-overflow-tooltip />
          <el-table-column label="联系电话" min-width="135"><template #default="scope">{{ scope.row.patientPhone || '—' }}</template></el-table-column>
          <el-table-column label="状态" width="104"><template #default="scope"><el-tag :type="statusMeta[scope.row.status as AppointmentStatus].type" effect="light" round>{{ statusMeta[scope.row.status as AppointmentStatus].text }}</el-tag></template></el-table-column>
          <el-table-column label="处理" fixed="right" width="210">
            <template #default="scope">
              <el-button v-if="scope.row.status === 'PENDING'" size="small" type="primary" :icon="Checked" @click="changeAppointmentStatus(scope.row, 'CONFIRMED')">确认</el-button>
              <el-button v-if="scope.row.status === 'CONFIRMED'" size="small" type="success" :icon="CircleCheck" @click="changeAppointmentStatus(scope.row, 'COMPLETED')">完成接诊</el-button>
              <el-dropdown v-if="scope.row.status === 'PENDING' || scope.row.status === 'CONFIRMED'" @command="changeAppointmentStatus(scope.row, $event as AppointmentStatus)">
                <el-button size="small" text>更多</el-button>
                <template #dropdown><el-dropdown-menu><el-dropdown-item command="CANCELLED">取消预约</el-dropdown-item></el-dropdown-menu></template>
              </el-dropdown>
              <span v-else class="handled-text">已处理</span>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <template v-else-if="activeSection === 'patients'">
      <section class="patient-search-banner surface">
        <div><span><el-icon><Search /></el-icon></span><div><h3>居民快速检索</h3><p>可按姓名、身份证号或联系方式查询居民健康档案</p></div></div>
        <el-input v-model="patientKeyword" size="large" clearable placeholder="输入姓名 / 身份证号 / 手机号" @keyup.enter="loadPatients" @clear="loadPatients">
          <template #append><el-button :icon="Search" :loading="patientsLoading" @click="loadPatients">查询</el-button></template>
        </el-input>
      </section>
      <section class="panel surface section-panel patient-results">
        <header class="panel-header"><div><h3>居民档案</h3><p>共找到 {{ patients.length }} 条记录</p></div></header>
        <div v-loading="patientsLoading" class="resident-list">
          <article v-for="patient in patients" :key="patient.id" class="resident-row">
            <span class="patient-avatar large">{{ patient.name.slice(0, 1) }}</span>
            <div class="resident-identity"><strong>{{ patient.name }}</strong><p>{{ patient.gender || '性别未知' }} · {{ patientAge(patient) }} · {{ maskIdCard(patient.idCard) }}</p></div>
            <div class="resident-contact"><span><el-icon><Phone /></el-icon>{{ patient.phone || '未登记电话' }}</span><span><el-icon><Location /></el-icon>{{ patient.address || '未登记地址' }}</span></div>
            <div class="resident-tags"><el-tag v-if="patient.chronicDiseaseCount" type="warning" effect="light">{{ patient.chronicDiseaseCount }} 项慢病</el-tag><span>最近监测 {{ formatDate(patient.lastHealthRecordAt) }}</span></div>
            <div class="resident-actions"><el-button :icon="View" @click="openPatient(patient)">健康档案</el-button><el-button type="primary" plain :icon="Plus" @click="openHealthDialog(patient)">登记数据</el-button></div>
          </article>
          <el-empty v-if="!patients.length && !patientsLoading" :description="moduleStates.patients === 'error' ? '居民档案加载失败，请重试' : '未找到匹配的居民'" />
        </div>
      </section>
    </template>

    <template v-else-if="activeSection === 'health'">
      <section class="panel surface section-panel">
        <header class="section-toolbar">
          <div><h3>健康数据登记</h3><p>记录居民血压、心率、血氧、体重等健康指标</p></div>
          <el-button type="primary" :icon="DocumentAdd" @click="openHealthDialog()">新增健康记录</el-button>
        </header>
        <div class="metric-guide">
          <div><span class="metric-icon heart">♥</span><div><strong>心率</strong><small>成人静息参考 60–100 次/分</small></div></div>
          <div><span class="metric-icon pressure">BP</span><div><strong>血压</strong><small>关注收缩压与舒张压变化</small></div></div>
          <div><span class="metric-icon oxygen">O₂</span><div><strong>血氧</strong><small>一般建议维持在 95% 以上</small></div></div>
          <div><span class="metric-icon weight">kg</span><div><strong>体重</strong><small>持续记录有助于趋势判断</small></div></div>
        </div>
        <el-table v-loading="healthLoading" :data="healthRecords" class="portal-table" :empty-text="moduleStates.health === 'error' ? '健康记录加载失败，请重试' : '暂无健康数据'">
          <el-table-column label="居民" min-width="150"><template #default="scope"><b>{{ patientName(scope.row.patientId, scope.row.patientName) }}</b></template></el-table-column>
          <el-table-column label="记录时间" min-width="160"><template #default="scope">{{ formatDateTime(scope.row.recordedAt) }}</template></el-table-column>
          <el-table-column label="血压 (mmHg)" width="130"><template #default="scope"><strong>{{ scope.row.systolicPressure ?? '—' }}/{{ scope.row.diastolicPressure ?? '—' }}</strong></template></el-table-column>
          <el-table-column label="心率" width="105"><template #default="scope">{{ scope.row.heartRate ?? '—' }}<small v-if="scope.row.heartRate"> 次/分</small></template></el-table-column>
          <el-table-column label="血氧" width="100"><template #default="scope">{{ scope.row.bloodOxygen ?? '—' }}<small v-if="scope.row.bloodOxygen">%</small></template></el-table-column>
          <el-table-column label="体重" width="100"><template #default="scope">{{ scope.row.weight ?? '—' }}<small v-if="scope.row.weight"> kg</small></template></el-table-column>
          <el-table-column prop="note" label="备注" min-width="190" show-overflow-tooltip />
        </el-table>
      </section>
    </template>

    <template v-else-if="activeSection === 'chronic'">
      <section class="panel surface section-panel">
        <header class="section-toolbar">
          <div><h3>慢病居民管理</h3><p>掌握风险分层与管理方案，优先关注高风险居民</p></div>
          <el-input v-model="chronicKeyword" clearable placeholder="疾病类型 / 风险等级" :prefix-icon="Search" style="width: 300px" @keyup.enter="loadChronicCases" @clear="loadChronicCases"><template #append><el-button @click="loadChronicCases">查询</el-button></template></el-input>
        </header>
        <div class="risk-summary">
          <div class="risk-high"><b>{{ highRiskChronicCases.length }}</b><span>高风险居民</span><small>建议优先随访</small></div>
          <div><b>{{ chronicCases.length }}</b><span>在管慢病档案</span><small>持续规范管理</small></div>
          <div><b>{{ new Set(chronicCases.map(item => item.diseaseType)).size }}</b><span>慢病类型</span><small>覆盖服务病种</small></div>
        </div>
        <el-table v-loading="chronicLoading" :data="chronicCases" class="portal-table" :empty-text="moduleStates.chronic === 'error' ? '慢病档案加载失败，请重试' : '暂无慢病档案'">
          <el-table-column label="居民" min-width="145"><template #default="scope"><div class="person-cell compact"><span>{{ patientName(scope.row.patientId, scope.row.patientName).slice(0, 1) }}</span><div><b>{{ patientName(scope.row.patientId, scope.row.patientName) }}</b><small>档案 #{{ scope.row.patientId }}</small></div></div></template></el-table-column>
          <el-table-column prop="diseaseType" label="疾病类型" min-width="125" />
          <el-table-column label="风险分层" width="110"><template #default="scope"><el-tag :type="riskType(scope.row.riskLevel)" effect="light" round>{{ riskText(scope.row.riskLevel) }}</el-tag></template></el-table-column>
          <el-table-column label="确诊日期" width="125"><template #default="scope">{{ formatDate(scope.row.diagnosisDate) }}</template></el-table-column>
          <el-table-column prop="managementPlan" label="管理方案" min-width="260" show-overflow-tooltip />
          <el-table-column label="最近随访" width="125"><template #default="scope">{{ formatDate(scope.row.lastFollowUpAt) }}</template></el-table-column>
          <el-table-column label="下次随访" width="125"><template #default="scope">{{ formatDate(scope.row.nextFollowUpAt) }}</template></el-table-column>
        </el-table>
      </section>
    </template>

    <template v-else-if="activeSection === 'inventory'">
      <section class="inventory-hero surface">
        <div class="inventory-title"><span><el-icon><Bell /></el-icon></span><div><h3>药品库存预警</h3><p>以下药品已达到或低于安全库存，请核对实际数量并及时反馈药房。</p></div></div>
        <div class="inventory-count"><b>{{ medicineAlerts.length }}</b><span>种药品需关注</span></div>
      </section>
      <div v-loading="inventoryLoading" class="medicine-grid">
        <article v-for="medicine in medicineAlerts" :key="medicine.id" class="medicine-card surface" :class="{ critical: medicine.stock === 0 }">
          <header><span class="medicine-icon"><el-icon><Box /></el-icon></span><el-tag :type="medicine.stock === 0 ? 'danger' : 'warning'" effect="dark" size="small">{{ medicine.stock === 0 ? '已缺货' : '库存偏低' }}</el-tag></header>
          <h4>{{ medicine.name }}</h4><p>{{ medicine.specification || medicine.category || '规格未登记' }}</p>
          <div class="stock-numbers"><div><small>当前库存</small><b>{{ medicine.stock }}</b></div><div><small>安全库存</small><strong>{{ medicine.minimumStock }}</strong></div></div>
          <el-progress :percentage="stockPercent(medicine)" :show-text="false" :stroke-width="7" :color="medicine.stock === 0 ? '#d65259' : '#e39943'" />
          <footer><span>缺口 {{ Math.max(0, medicine.minimumStock - medicine.stock) }}</span><span>{{ medicine.category || '药品' }}</span></footer>
        </article>
        <el-empty v-if="!medicineAlerts.length && !inventoryLoading" :description="moduleStates.inventory === 'error' ? '库存预警加载失败，请重试' : '当前药品库存充足'" class="surface medicine-empty" />
      </div>
    </template>

    <el-dialog v-model="healthDialogVisible" title="登记居民健康数据" width="680px" destroy-on-close class="health-dialog">
      <div class="dialog-intro"><span><el-icon><TrendCharts /></el-icon></span><div><strong>基础健康监测</strong><p>请按实际测量结果填写，未测量的指标可留空。</p></div></div>
      <el-form ref="healthFormRef" :model="healthForm" :rules="healthRules" label-position="top">
        <div class="health-form-grid">
          <el-form-item label="居民" prop="patientId">
            <el-select v-model="healthForm.patientId" filterable placeholder="输入姓名选择居民" style="width: 100%">
              <el-option v-for="patient in patients" :key="patient.id" :label="`${patient.name} · ${patient.phone || '无电话'}`" :value="patient.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="测量时间" prop="recordedAt">
            <el-date-picker v-model="healthForm.recordedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="收缩压 (mmHg)"><el-input-number v-model="healthForm.systolicPressure" :min="40" :max="260" :controls="false" placeholder="如 120" style="width: 100%" /></el-form-item>
          <el-form-item label="舒张压 (mmHg)"><el-input-number v-model="healthForm.diastolicPressure" :min="30" :max="180" :controls="false" placeholder="如 80" style="width: 100%" /></el-form-item>
          <el-form-item label="心率 (次/分)"><el-input-number v-model="healthForm.heartRate" :min="20" :max="250" :controls="false" placeholder="如 72" style="width: 100%" /></el-form-item>
          <el-form-item label="血氧 (%)"><el-input-number v-model="healthForm.bloodOxygen" :min="50" :max="100" :controls="false" placeholder="如 98" style="width: 100%" /></el-form-item>
          <el-form-item label="体重 (kg)"><el-input-number v-model="healthForm.weight" :min="20" :max="250" :precision="1" :controls="false" placeholder="如 65.5" style="width: 100%" /></el-form-item>
          <el-form-item label="备注" class="wide"><el-input v-model="healthForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可填写居民当前症状、测量条件或后续建议" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="healthDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingHealth" @click="saveHealthRecord">保存健康记录</el-button></template>
    </el-dialog>

    <el-drawer v-model="patientDrawerVisible" size="560px" destroy-on-close>
      <template #header><div class="drawer-title"><span class="patient-avatar large">{{ selectedPatient?.name?.slice(0, 1) }}</span><div><h3>{{ selectedPatient?.name }}</h3><p>居民健康档案 #{{ selectedPatient?.id }}</p></div></div></template>
      <template v-if="selectedPatient">
        <section class="profile-card">
          <div><span>基本信息</span><strong>{{ selectedPatient.gender || '—' }} · {{ patientAge(selectedPatient) }}</strong></div>
          <div><span>联系电话</span><strong>{{ selectedPatient.phone || '—' }}</strong></div>
          <div class="wide"><span>身份证号</span><strong>{{ maskIdCard(selectedPatient.idCard) }}</strong></div>
          <div class="wide"><span>家庭地址</span><strong>{{ selectedPatient.address || '—' }}</strong></div>
        </section>
        <div class="drawer-section-title"><div><h4>最近健康监测</h4><p>查看居民关键健康指标</p></div><el-button type="primary" size="small" :icon="Plus" @click="openHealthDialog(selectedPatient)">登记数据</el-button></div>
        <div v-if="selectedPatientRecords.length" class="record-stack">
          <article v-for="record in selectedPatientRecords.slice(0, 5)" :key="record.id || record.recordedAt">
            <header><strong>{{ formatDateTime(record.recordedAt) }}</strong><span>{{ record.note || '常规健康监测' }}</span></header>
            <div><span><small>血压</small><b>{{ record.systolicPressure ?? '—' }}/{{ record.diastolicPressure ?? '—' }}</b></span><span><small>心率</small><b>{{ record.heartRate ?? '—' }}</b></span><span><small>血氧</small><b>{{ record.bloodOxygen ?? '—' }}%</b></span><span><small>体重</small><b>{{ record.weight ?? '—' }} kg</b></span></div>
          </article>
        </div>
        <el-empty v-else description="该居民暂无健康监测记录" :image-size="72" />
        <div class="drawer-section-title"><div><h4>慢病档案</h4><p>当前在管慢病与风险分层</p></div></div>
        <div v-if="selectedPatientChronicCases.length" class="chronic-stack">
          <article v-for="item in selectedPatientChronicCases" :key="item.id"><div><strong>{{ item.diseaseType }}</strong><p>{{ item.managementPlan || '暂未填写管理方案' }}</p></div><el-tag :type="riskType(item.riskLevel)" effect="light">{{ riskText(item.riskLevel) }}</el-tag></article>
        </div>
        <el-empty v-else description="该居民暂无慢病档案" :image-size="72" />
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.staff-portal{--staff-primary:#126b63;--staff-ink:#183d3a;--staff-muted:#768e8b;display:grid;gap:18px;color:var(--staff-ink)}
.portal-hero{min-height:112px;padding:23px 26px;display:flex;align-items:center;justify-content:space-between;border-radius:22px;color:white;background:radial-gradient(circle at 78% 10%,rgba(111,211,185,.32),transparent 29%),linear-gradient(125deg,#0c5e59,#13796f 68%,#16877c);box-shadow:0 15px 36px rgba(12,94,89,.18)}
.hero-kicker{display:flex;align-items:center;gap:8px;color:#c4eee4;font-size:12px;letter-spacing:.08em}.hero-kicker i{width:7px;height:7px;border-radius:50%;background:#80e2c7;box-shadow:0 0 0 5px rgba(128,226,199,.16)}
.portal-hero h2{margin:9px 0 5px;font-size:24px;letter-spacing:-.02em}.portal-hero p{margin:0;color:#c9e7e2;font-size:13px}.hero-actions{display:flex;gap:10px}.hero-actions :deep(.el-button.is-plain){color:#f2fffc;border-color:rgba(255,255,255,.36);background:rgba(255,255,255,.1)}.hero-actions :deep(.el-button--primary){color:#0c625b;border-color:white;background:white}
.portal-navigation{padding:8px;display:grid;grid-template-columns:repeat(6,1fr);gap:6px}.portal-navigation button{position:relative;height:54px;padding:0 12px;display:flex;align-items:center;justify-content:center;gap:8px;border:0;border-radius:12px;color:#66817e;background:transparent;cursor:pointer;transition:.18s}.portal-navigation button:hover{color:var(--staff-primary);background:#f2f8f6}.portal-navigation button.active{color:#0c655e;background:#e7f4f1;font-weight:700}.portal-navigation button .el-icon{font-size:17px}.portal-navigation button b{min-width:19px;height:19px;padding:0 5px;display:grid;place-items:center;border-radius:10px;color:#9b621e;background:#ffebcb;font-size:10px}.portal-navigation button b.danger{color:#a64249;background:#fde0e3}
.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:15px}.summary-card{padding:19px;display:flex;align-items:center;gap:15px}.summary-icon{width:49px;height:49px;display:grid;place-items:center;flex:none;border-radius:15px;font-size:23px}.summary-card small,.summary-card strong,.summary-card p{display:block}.summary-card small{color:#78918e;font-size:12px}.summary-card strong{margin-top:2px;font-size:28px;line-height:1}.summary-card p{margin:6px 0 0;color:#91a29f;font-size:11px}
.overview-grid{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(330px,.85fr);gap:18px}.panel{padding:21px}.panel-header,.section-toolbar{display:flex;align-items:center;justify-content:space-between;gap:18px}.panel-header{margin-bottom:16px}.panel-header h3,.section-toolbar h3{margin:0 0 5px;font-size:17px}.panel-header p,.section-toolbar p{margin:0;color:#839997;font-size:12px}.timeline-list{display:grid}.timeline-item{min-height:62px;display:grid;grid-template-columns:67px 17px 1fr;align-items:center}.appointment-time b,.appointment-time span{display:block}.appointment-time b{font-size:15px}.appointment-time span{margin-top:4px;color:#93a4a1;font-size:10px}.timeline-dot{position:relative;width:9px;height:9px;border:2px solid white;border-radius:50%;background:#e4aa58;box-shadow:0 0 0 3px #f9e8d1}.timeline-dot.confirmed{background:#3ca582;box-shadow:0 0 0 3px #d9f1e8}.timeline-dot:after{content:'';position:absolute;top:10px;left:2px;width:1px;height:48px;background:#e6eeec}.timeline-item:last-child .timeline-dot:after{display:none}.appointment-main{min-width:0;padding:10px 0 10px 5px;display:flex;align-items:center;justify-content:space-between;gap:12px;border-bottom:1px solid #eff3f2}.appointment-main strong,.appointment-main span{display:block}.appointment-main strong{font-size:14px}.appointment-main span{margin-top:4px;color:#849a97;font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.appointment-main>div{min-width:0}.reminder-stack{display:grid;gap:10px}.reminder{width:100%;padding:14px;display:grid;grid-template-columns:42px 1fr 18px;align-items:center;gap:12px;border:0;border-radius:14px;text-align:left;cursor:pointer}.reminder>span{width:40px;height:40px;display:grid;place-items:center;border-radius:12px;font-size:18px}.reminder>div strong,.reminder>div p{display:block}.reminder>div strong{font-size:13px}.reminder>div p{margin:4px 0 0;color:#84938f;font-size:10px}.reminder.pending{background:#fff7eb}.reminder.pending>span{color:#a86b20;background:#ffe9c8}.reminder.risk{background:#fdf0f1}.reminder.risk>span{color:#af4b52;background:#fbdcdf}.reminder.stock{background:#eef6f4}.reminder.stock>span{color:#196f65;background:#dceee9}.reminder>.el-icon{color:#9cafac}
.quick-patients{padding:21px}.patient-card-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.mini-patient-card{min-width:0;padding:14px;display:grid;grid-template-columns:40px 1fr;gap:10px 11px;align-items:center;border:1px solid #e8efed;border-radius:14px}.patient-avatar{width:40px;height:40px;display:grid;place-items:center;flex:none;border-radius:13px;color:#116a62;background:#dff1ed;font-weight:800}.patient-avatar.large{width:48px;height:48px;border-radius:15px;font-size:17px}.mini-patient-info{min-width:0}.mini-patient-info strong,.mini-patient-info p{display:block}.mini-patient-info p{margin:4px 0 0;color:#849794;font-size:11px}.mini-patient-card .el-button{margin:0}
.section-panel{padding:0;overflow:hidden}.section-toolbar{padding:20px 22px;border-bottom:1px solid #edf2f1}.portal-table{padding:5px 18px 16px}.table-time b,.table-time span{display:block}.table-time b{font-size:15px}.table-time span{margin-top:2px;color:#91a39f;font-size:10px}.person-cell{display:flex;align-items:center;gap:10px}.person-cell>span{width:34px;height:34px;display:grid;place-items:center;flex:none;border-radius:11px;color:#146c64;background:#e3f2ef;font-weight:700}.person-cell b,.person-cell small{display:block}.person-cell small{margin-top:3px;color:#91a19f;font-size:10px}.person-cell.compact>span{width:32px;height:32px}.handled-text{color:#94a3a1;font-size:12px}
.patient-search-banner{padding:22px 24px;display:grid;grid-template-columns:1fr minmax(390px,.8fr);align-items:center;gap:28px}.patient-search-banner>div{display:flex;align-items:center;gap:14px}.patient-search-banner>div>span{width:46px;height:46px;display:grid;place-items:center;border-radius:14px;color:#126b63;background:#e2f2ee;font-size:20px}.patient-search-banner h3{margin:0 0 5px;font-size:18px}.patient-search-banner p{margin:0;color:#819694;font-size:12px}.patient-results{overflow:visible}.resident-list{padding:0 22px 18px}.resident-row{min-height:88px;display:grid;grid-template-columns:48px minmax(150px,1fr) minmax(220px,1.45fr) minmax(145px,.8fr) auto;align-items:center;gap:15px;border-bottom:1px solid #edf2f1}.resident-row:last-child{border-bottom:0}.resident-identity strong{font-size:14px}.resident-identity p{margin:5px 0 0;color:#839795;font-size:11px}.resident-contact{min-width:0;display:grid;gap:6px}.resident-contact span{display:flex;align-items:center;gap:6px;color:#687f7c;font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.resident-contact .el-icon{color:#94aaa7}.resident-tags{display:grid;justify-items:start;gap:5px}.resident-tags>span{color:#91a3a0;font-size:10px}.resident-actions{display:flex;gap:8px}.resident-actions .el-button{margin:0}
.metric-guide{margin:18px 20px;display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.metric-guide>div{padding:13px;display:flex;align-items:center;gap:10px;border-radius:13px;background:#f7faf9}.metric-icon{width:35px;height:35px;display:grid;place-items:center;flex:none;border-radius:11px;font-size:11px;font-weight:800}.metric-icon.heart{color:#b64850;background:#fde2e4;font-size:17px}.metric-icon.pressure{color:#7655a1;background:#eee8f7}.metric-icon.oxygen{color:#237096;background:#e1f1f9}.metric-icon.weight{color:#26775d;background:#dff1e8}.metric-guide strong,.metric-guide small{display:block}.metric-guide strong{font-size:12px}.metric-guide small{margin-top:3px;color:#899b98;font-size:9px}
.risk-summary{margin:18px 20px;display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.risk-summary>div{padding:15px 17px;display:grid;grid-template-columns:auto 1fr;align-items:center;column-gap:10px;border-radius:14px;background:#f3f8f6}.risk-summary>div.risk-high{background:#fff0f1}.risk-summary b{grid-row:1/3;font-size:25px}.risk-summary span{font-size:12px;font-weight:700}.risk-summary small{color:#8ca09d;font-size:10px}.risk-high b{color:#ad454d}
.inventory-hero{padding:22px 24px;display:flex;align-items:center;justify-content:space-between;border-color:#f2d8d9;background:linear-gradient(110deg,#fff8f7,#fff)}.inventory-title{display:flex;align-items:center;gap:14px}.inventory-title>span{width:48px;height:48px;display:grid;place-items:center;border-radius:15px;color:#b34850;background:#fbe0e2;font-size:21px}.inventory-title h3{margin:0 0 6px;font-size:18px}.inventory-title p{margin:0;color:#8e7777;font-size:12px}.inventory-count{text-align:right}.inventory-count b,.inventory-count span{display:block}.inventory-count b{color:#b4474e;font-size:29px}.inventory-count span{color:#9e8585;font-size:10px}.medicine-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:15px}.medicine-card{min-width:0;padding:19px;overflow:hidden}.medicine-card.critical{border-color:#f0cfd1}.medicine-card header{display:flex;align-items:center;justify-content:space-between;gap:12px}.medicine-card header :deep(.el-tag){flex:none;white-space:nowrap}.medicine-card .medicine-icon{width:38px;height:38px;display:grid;place-items:center;border-radius:12px;color:#9d6529;background:#fff0dc;font-size:17px}.medicine-card.critical .medicine-icon{color:#b2444c;background:#fbe0e2}.medicine-card h4{margin:14px 0 5px;font-size:15px}.medicine-card>p{height:17px;margin:0;color:#8a9a97;font-size:11px}.stock-numbers{margin:16px 0 10px;display:flex;justify-content:space-between}.stock-numbers small,.stock-numbers b,.stock-numbers strong{display:block}.stock-numbers small{color:#91a19e;font-size:10px}.stock-numbers b{margin-top:3px;color:#b34a50;font-size:22px}.stock-numbers strong{margin-top:6px;font-size:14px}.medicine-card footer{margin-top:10px;display:flex;justify-content:space-between;color:#8b9d9a;font-size:10px}.medicine-empty{grid-column:1/-1;min-height:240px}
.dialog-intro{margin:-4px 0 18px;padding:13px;display:flex;align-items:center;gap:11px;border-radius:13px;background:#eff7f5}.dialog-intro>span{width:37px;height:37px;display:grid;place-items:center;border-radius:11px;color:#116a62;background:#d9ece7;font-size:17px}.dialog-intro strong,.dialog-intro p{display:block}.dialog-intro strong{font-size:13px}.dialog-intro p{margin:4px 0 0;color:#7a918e;font-size:10px}.health-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 18px}.health-form-grid .wide{grid-column:1/-1}
.drawer-title{display:flex;align-items:center;gap:12px}.drawer-title h3{margin:0 0 4px;font-size:18px}.drawer-title p{margin:0;color:#8b9b99;font-size:11px}.profile-card{padding:17px;display:grid;grid-template-columns:1fr 1fr;gap:15px;border-radius:16px;background:#f4f8f7}.profile-card>div{display:grid;gap:4px}.profile-card .wide{grid-column:1/-1}.profile-card span{color:#8c9e9b;font-size:10px}.profile-card strong{font-size:12px}.drawer-section-title{margin:24px 0 12px;display:flex;align-items:center;justify-content:space-between}.drawer-section-title h4{margin:0 0 4px;font-size:15px}.drawer-section-title p{margin:0;color:#90a09e;font-size:10px}.record-stack,.chronic-stack{display:grid;gap:10px}.record-stack article,.chronic-stack article{padding:14px;border:1px solid #e7efed;border-radius:13px}.record-stack header{display:flex;justify-content:space-between;gap:12px}.record-stack header strong{font-size:11px}.record-stack header span{color:#8c9d9a;font-size:10px}.record-stack article>div{margin-top:12px;display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.record-stack article>div>span{display:grid;gap:3px}.record-stack small{color:#91a19e;font-size:9px}.record-stack b{font-size:12px}.chronic-stack article{display:flex;align-items:center;justify-content:space-between;gap:14px}.chronic-stack strong{font-size:13px}.chronic-stack p{margin:5px 0 0;color:#839693;font-size:10px}
@media(max-width:1280px){.portal-navigation{grid-template-columns:repeat(3,1fr)}.summary-grid{grid-template-columns:repeat(2,1fr)}.patient-card-grid{grid-template-columns:repeat(2,1fr)}.medicine-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.resident-row{grid-template-columns:48px minmax(130px,1fr) minmax(190px,1.2fr) auto}.resident-tags{display:none}}
</style>
