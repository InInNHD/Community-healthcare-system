<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Calendar,
  CircleCheck,
  Clock,
  DataLine,
  EditPen,
  FirstAidKit,
  HomeFilled,
  Location,
  Memo,
  Odometer,
  Phone,
  Plus,
  Refresh,
  TrendCharts,
  User,
  Warning,
  SwitchButton,
} from '@element-plus/icons-vue'
import { http, type PageResult } from '../api/http'
import { logout } from '../auth'
import { hasBloodPressureRecordedToday, mapTrendValue, measurementStatus, splitRecordedSeries } from '../features/resident/health-presentation'
import { portalConfig } from '../config/portal-config'
import { ResidentPortalControllerService, type ResidentAppointmentRequest } from '../api/generated'
import { generatedApiErrorMessage } from '../api/generated-client'
import {
  createModuleStates,
  failedModuleNames,
  markModulesLoading,
  resolveModuleState,
  type PortalModuleState,
} from '../features/portal/module-state'

type PortalSection = 'overview' | 'appointments' | 'health' | 'chronic' | 'profile'
type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'

interface ResidentProfile {
  id?: number
  name: string
  gender: string
  birthDate: string
  phone: string
  address: string
  idCard?: string
  bloodType?: string
}

interface Doctor {
  id: number
  name: string
  department?: string
  specialty?: string
  title?: string
  phone?: string
}

interface Appointment {
  id: number
  appointmentNo?: string
  doctorId: number
  doctorName?: string
  department?: string
  scheduledAt: string
  status: AppointmentStatus
  reason: string
  remark?: string
}

interface HealthRecord {
  id: number
  recordedAt: string
  systolicPressure?: number | null
  diastolicPressure?: number | null
  heartRate?: number | null
  bloodOxygen?: number | null
  weight?: number | null
  note?: string
}

interface ChronicPlan {
  id: number
  diseaseType: string
  riskLevel?: string
  diagnosisDate?: string
  doctorId?: number
  doctorName?: string
  managementPlan?: string
  nextFollowUp?: string
  active?: boolean
}

interface ResidentOverview {
  activeChronicCases?: number
  chronicPlanCount?: number
  pendingAppointments?: number
  healthRecordCount?: number
  upcomingAppointment?: Appointment | null
  nextAppointment?: Appointment | null
  latestHealthRecord?: HealthRecord | null
  profile?: Partial<ResidentProfile>
}

interface ListPayload<T> extends Partial<PageResult<T>> {
  content?: T[]
  records?: T[]
}

const navItems = [
  { key: 'overview' as const, label: '健康首页', icon: HomeFilled },
  { key: 'appointments' as const, label: '我的预约', icon: Calendar },
  { key: 'health' as const, label: '健康监测', icon: TrendCharts },
  { key: 'chronic' as const, label: '慢病计划', icon: Memo },
  { key: 'profile' as const, label: '个人资料', icon: User },
]

const statusMeta: Record<AppointmentStatus, { label: string; tone: string }> = {
  PENDING: { label: '待确认', tone: 'amber' },
  CONFIRMED: { label: '已确认', tone: 'green' },
  COMPLETED: { label: '已完成', tone: 'blue' },
  CANCELLED: { label: '已取消', tone: 'gray' },
}

const activeSection = ref<PortalSection>('overview')
const residentModuleKeys = ['overview', 'profile', 'appointments', 'health', 'chronic', 'doctors'] as const
type ResidentModuleKey = typeof residentModuleKeys[number]
const router = useRouter()
const loading = ref(false)
const refreshing = ref(false)
const portalLoadErrors = ref<string[]>([])
const moduleStates = reactive<Record<ResidentModuleKey, PortalModuleState>>(createModuleStates(residentModuleKeys))
const appointmentDialogVisible = ref(false)
const healthDialogVisible = ref(false)
const appointmentSaving = ref(false)
const healthSaving = ref(false)
const profileSaving = ref(false)
const appointmentFormRef = ref<any>()
const healthFormRef = ref<any>()
const profileFormRef = ref<any>()

const overview = ref<ResidentOverview>({})
const appointments = ref<Appointment[]>([])
const healthRecords = ref<HealthRecord[]>([])
const chronicPlans = ref<ChronicPlan[]>([])
const doctors = ref<Doctor[]>([])
const profile = reactive<ResidentProfile>({
  name: '',
  gender: '',
  birthDate: '',
  phone: '',
  address: '',
})
const profileDraft = reactive({ phone: '', address: '' })
const appointmentForm = reactive<{ doctorId?: number; scheduledAt: string; reason: string }>({
  doctorId: undefined,
  scheduledAt: '',
  reason: '',
})
const healthForm = reactive<{
  systolicPressure?: number
  diastolicPressure?: number
  heartRate?: number
  bloodOxygen?: number
  weight?: number
  note: string
}>({ note: '' })

const appointmentRules = {
  doctorId: [{ required: true, message: '请选择接诊医生', trigger: 'change' }],
  scheduledAt: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  reason: [
    { required: true, message: '请简要填写就诊原因', trigger: 'blur' },
    { min: 2, max: 500, message: '就诊原因应为 2 至 500 个字符', trigger: 'blur' },
  ],
}
const healthRules = {
  systolicPressure: [{ required: true, message: '请填写收缩压', trigger: 'blur' }],
  diastolicPressure: [{ required: true, message: '请填写舒张压', trigger: 'blur' }],
}
const profileRules = {
  phone: [
    { required: true, message: '请填写手机号码', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号码', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请填写居住地址', trigger: 'blur' }],
}

function normalizeList<T>(payload: T[] | ListPayload<T> | undefined | null): T[] {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  return payload.items ?? payload.content ?? payload.records ?? []
}

function parseDate(value?: string) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDate(value?: string) {
  const date = parseDate(value)
  if (!date) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(date)
}

function formatDateTime(value?: string) {
  const date = parseDate(value)
  if (!date) return '时间待定'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function formatRecordTime(value?: string) {
  const date = parseDate(value)
  if (!date) return '暂无记录时间'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function getDoctorName(appointment: Appointment) {
  return appointment.doctorName || doctors.value.find(item => item.id === appointment.doctorId)?.name || `医生 ${appointment.doctorId}`
}

function getPlanDoctor(plan: ChronicPlan) {
  return plan.doctorName || doctors.value.find(item => item.id === plan.doctorId)?.name || '社区家庭医生'
}

const currentDateText = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date()))

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const residentFirstName = computed(() => profile.name?.slice(0, 1) || '您')
const sortedAppointments = computed(() => [...appointments.value].sort((a, b) => {
  return (parseDate(b.scheduledAt)?.getTime() ?? 0) - (parseDate(a.scheduledAt)?.getTime() ?? 0)
}))
const activeAppointments = computed(() => sortedAppointments.value.filter(item => item.status === 'PENDING' || item.status === 'CONFIRMED'))
const upcomingAppointment = computed(() => {
  if (overview.value.nextAppointment) return overview.value.nextAppointment
  if (overview.value.upcomingAppointment) return overview.value.upcomingAppointment
  const now = Date.now()
  return [...activeAppointments.value]
    .filter(item => (parseDate(item.scheduledAt)?.getTime() ?? 0) >= now)
    .sort((a, b) => (parseDate(a.scheduledAt)?.getTime() ?? 0) - (parseDate(b.scheduledAt)?.getTime() ?? 0))[0] ?? null
})
const latestHealth = computed(() => {
  if (overview.value.latestHealthRecord) return overview.value.latestHealthRecord
  return [...healthRecords.value].sort((a, b) => {
    return (parseDate(b.recordedAt)?.getTime() ?? 0) - (parseDate(a.recordedAt)?.getTime() ?? 0)
  })[0] ?? null
})
const activePlanCount = computed(() => overview.value.activeChronicCases
  ?? overview.value.chronicPlanCount
  ?? chronicPlans.value.filter(item => item.active !== false).length)
const activeModuleState = computed(() => moduleStates[activeSection.value])
const activeModuleIssue = computed(() => {
  if (activeModuleState.value === 'error') return '当前模块尚未成功加载，请稍后重试。'
  if (activeModuleState.value === 'stale') return '当前显示上次成功加载的数据，本次刷新失败。'
  return ''
})
function indicatorState(...values: Array<number | null | undefined>) {
  const label = measurementStatus(...values)
  return { label, tone: label === '已记录' ? 'recorded' : label === '数据不完整' ? 'warning' : 'neutral' }
}

const healthIndicators = computed(() => {
  const record = latestHealth.value
  return [
    {
      key: 'pressure', label: '血压', icon: Odometer,
      value: record?.systolicPressure && record.diastolicPressure ? `${record.systolicPressure}/${record.diastolicPressure}` : '--/--',
      unit: 'mmHg', state: indicatorState(record?.systolicPressure, record?.diastolicPressure), tint: '#e85d75',
    },
    {
      key: 'heartRate', label: '静息心率', icon: DataLine,
      value: record?.heartRate ?? '--', unit: '次/分', state: indicatorState(record?.heartRate), tint: '#7b69d6',
    },
    {
      key: 'oxygen', label: '血氧饱和度', icon: FirstAidKit,
      value: record?.bloodOxygen ?? '--', unit: '%', state: indicatorState(record?.bloodOxygen), tint: '#2c91ca',
    },
    {
      key: 'weight', label: '体重', icon: TrendCharts,
      value: record?.weight ?? '--', unit: 'kg', state: indicatorState(record?.weight), tint: '#1f9a7a',
    },
  ]
})

const recentHealthRecords = computed(() => [...healthRecords.value]
  .sort((a, b) => (parseDate(b.recordedAt)?.getTime() ?? 0) - (parseDate(a.recordedAt)?.getTime() ?? 0))
  .slice(0, 12))

const chronologicalRecords = computed(() => [...healthRecords.value]
  .filter(item => item.systolicPressure || item.diastolicPressure)
  .sort((a, b) => (parseDate(a.recordedAt)?.getTime() ?? 0) - (parseDate(b.recordedAt)?.getTime() ?? 0))
  .slice(-7))

const trendValues = computed(() => chronologicalRecords.value.flatMap(item => [item.systolicPressure, item.diastolicPressure])
  .filter((value): value is number => typeof value === 'number' && Number.isFinite(value)))

function trendX(index: number) {
  const count = chronologicalRecords.value.length
  return count === 1 ? 170 : 16 + (index / (count - 1)) * 328
}

function trendSegments(key: 'systolicPressure' | 'diastolicPressure') {
  const data = chronologicalRecords.value
  if (!data.length) return []
  if (!trendValues.value.length) return []
  return splitRecordedSeries(data, key).map(segment => segment.map(point => {
    return `${trendX(point.index).toFixed(1)},${mapTrendValue(point.value, trendValues.value).toFixed(1)}`
  }).join(' '))
}

function trendPoints(key: 'systolicPressure' | 'diastolicPressure') {
  return chronologicalRecords.value.flatMap((record, index) => {
    const value = record[key]
    return typeof value === 'number' && Number.isFinite(value)
      ? [{ x: trendX(index), y: mapTrendValue(value, trendValues.value), index }]
      : []
  })
}

const hasHealthRecordToday = computed(() => hasBloodPressureRecordedToday(healthRecords.value))

function dayLabel(value: string) {
  const date = parseDate(value)
  return date ? `${date.getMonth() + 1}/${date.getDate()}` : '--'
}

function goTo(section: PortalSection) {
  activeSection.value = section
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openAppointmentDialog() {
  appointmentForm.doctorId = undefined
  appointmentForm.scheduledAt = ''
  appointmentForm.reason = ''
  appointmentDialogVisible.value = true
}

function openHealthDialog() {
  Object.assign(healthForm, {
    systolicPressure: undefined,
    diastolicPressure: undefined,
    heartRate: undefined,
    bloodOxygen: undefined,
    weight: undefined,
    note: '',
  })
  healthDialogVisible.value = true
}

async function loadPortal(showSuccess = false) {
  if (showSuccess) refreshing.value = true
  else loading.value = true
  const hadPreviousData: Record<ResidentModuleKey, boolean> = {
    overview: Object.keys(overview.value).length > 0,
    profile: Boolean(profile.id || profile.name),
    appointments: appointments.value.length > 0,
    health: healthRecords.value.length > 0,
    chronic: chronicPlans.value.length > 0,
    doctors: doctors.value.length > 0,
  }
  Object.assign(moduleStates, markModulesLoading(moduleStates, residentModuleKeys))
  try {
    const results = await Promise.allSettled([
      http.get<ResidentOverview>('/resident/overview'),
      http.get<ResidentProfile>('/resident/profile'),
      http.get<Appointment[] | ListPayload<Appointment>>('/resident/appointments'),
      http.get<HealthRecord[] | ListPayload<HealthRecord>>('/resident/health-records'),
      http.get<ChronicPlan[] | ListPayload<ChronicPlan>>('/resident/chronic-plans'),
      http.get<Doctor[] | ListPayload<Doctor>>('/resident/doctors'),
    ])
    const moduleNames = ['健康概览', '个人资料', '预约记录', '健康记录', '慢病计划', '医生列表']
    portalLoadErrors.value = failedModuleNames(results, moduleNames)
    if (results[0].status === 'fulfilled') overview.value = results[0].value.data ?? {}
    if (results[1].status === 'fulfilled') {
      Object.assign(profile, results[1].value.data)
      profileDraft.phone = profile.phone ?? ''
      profileDraft.address = profile.address ?? ''
    } else if (overview.value.profile) {
      Object.assign(profile, overview.value.profile)
      profileDraft.phone = profile.phone ?? ''
      profileDraft.address = profile.address ?? ''
    }
    if (results[2].status === 'fulfilled') appointments.value = normalizeList(results[2].value.data)
    if (results[3].status === 'fulfilled') healthRecords.value = normalizeList(results[3].value.data)
    if (results[4].status === 'fulfilled') chronicPlans.value = normalizeList(results[4].value.data)
    if (results[5].status === 'fulfilled') doctors.value = normalizeList(results[5].value.data)
    const isEmpty: Record<ResidentModuleKey, boolean> = {
      overview: Object.keys(overview.value).length === 0,
      profile: !profile.id && !profile.name,
      appointments: appointments.value.length === 0,
      health: healthRecords.value.length === 0,
      chronic: chronicPlans.value.length === 0,
      doctors: doctors.value.length === 0,
    }
    residentModuleKeys.forEach((key, index) => {
      moduleStates[key] = resolveModuleState(results[index], hadPreviousData[key], isEmpty[key])
    })
    if (showSuccess && portalLoadErrors.value.length === 0) ElMessage.success('健康数据已更新')
    else if (showSuccess) ElMessage.warning(`部分数据更新失败：${portalLoadErrors.value.join('、')}`)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function submitAppointment() {
  if (!(await appointmentFormRef.value?.validate())) return
  appointmentSaving.value = true
  try {
    const requestBody: ResidentAppointmentRequest = {
      doctorId: appointmentForm.doctorId!,
      scheduledAt: appointmentForm.scheduledAt,
      reason: appointmentForm.reason.trim(),
    }
    await ResidentPortalControllerService.createAppointment({ requestBody })
    ElMessage.success('预约申请已提交，请留意确认结果')
    appointmentDialogVisible.value = false
    const { data } = await http.get<Appointment[] | ListPayload<Appointment>>('/resident/appointments')
    appointments.value = normalizeList(data)
    activeSection.value = 'appointments'
  } catch (error) {
    ElMessage.error(generatedApiErrorMessage(error))
  } finally {
    appointmentSaving.value = false
  }
}

async function cancelAppointment(item: Appointment) {
  await ElMessageBox.confirm(
    `确定取消 ${formatDateTime(item.scheduledAt)} 与${getDoctorName(item)}的预约吗？`,
    '取消预约',
    { confirmButtonText: '确认取消', cancelButtonText: '保留预约', type: 'warning' },
  )
  await http.patch(`/resident/appointments/${item.id}/cancel`)
  item.status = 'CANCELLED'
  ElMessage.success('预约已取消')
}

function compactHealthPayload() {
  const now = new Date()
  const localRecordedAt = new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 19)
  return Object.fromEntries(Object.entries({
    recordedAt: localRecordedAt,
    systolicPressure: healthForm.systolicPressure,
    diastolicPressure: healthForm.diastolicPressure,
    heartRate: healthForm.heartRate,
    bloodOxygen: healthForm.bloodOxygen,
    weight: healthForm.weight,
    note: healthForm.note.trim() || undefined,
  }).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

async function submitHealthRecord() {
  if (!(await healthFormRef.value?.validate())) return
  healthSaving.value = true
  try {
    await http.post('/resident/health-records', compactHealthPayload())
    ElMessage.success('健康指标上报成功')
    healthDialogVisible.value = false
    const { data } = await http.get<HealthRecord[] | ListPayload<HealthRecord>>('/resident/health-records')
    healthRecords.value = normalizeList(data)
    activeSection.value = 'health'
  } finally {
    healthSaving.value = false
  }
}

async function saveProfile() {
  if (!(await profileFormRef.value?.validate())) return
  profileSaving.value = true
  try {
    const { data } = await http.put<ResidentProfile>('/resident/profile', {
      phone: profileDraft.phone,
      address: profileDraft.address,
    })
    profile.phone = data?.phone ?? profileDraft.phone
    profile.address = data?.address ?? profileDraft.address
    ElMessage.success('个人资料已保存')
  } finally {
    profileSaving.value = false
  }
}

onMounted(() => loadPortal())
async function signOut() { await logout(); await router.replace({ path: '/login', query: { portal: 'resident' } }) }
</script>

<template>
  <div v-loading="loading" class="resident-portal">
    <header class="portal-header">
      <div class="portal-header__inner">
        <button class="portal-brand" type="button" @click="goTo('overview')">
          <span class="portal-brand__mark"><el-icon><FirstAidKit /></el-icon></span>
          <span><b>{{ portalConfig.organizationName }}</b><small>社区健康服务门户</small></span>
        </button>
        <nav class="portal-nav" aria-label="居民门户导航">
          <button
            v-for="item in navItems"
            :key="item.key"
            type="button"
            :class="{ active: activeSection === item.key }"
            @click="goTo(item.key)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>
        <div class="portal-actions">
          <button class="round-action" type="button" title="刷新数据" @click="loadPortal(true)">
            <el-icon :class="{ rotating: refreshing }"><Refresh /></el-icon>
          </button>
          <button class="resident-chip" type="button" @click="goTo('profile')">
            <span class="resident-avatar">{{ residentFirstName }}</span>
            <span><b>{{ profile.name || '居民用户' }}</b><small>个人健康档案</small></span>
          </button>
          <el-button class="logout-action" text :icon="SwitchButton" @click="signOut">退出</el-button>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <el-alert
        v-if="portalLoadErrors.length"
        class="portal-load-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="`部分模块加载失败：${portalLoadErrors.join('、')}`"
        description="已成功加载的内容仍可使用；失败模块若显示旧数据，会在刷新成功后更新。"
      />
      <el-alert
        v-if="activeModuleIssue"
        class="module-state-alert"
        :type="activeModuleState === 'error' ? 'error' : 'warning'"
        :closable="false"
        show-icon
        :title="activeModuleIssue"
      />
      <template v-if="activeSection === 'overview'">
        <section class="welcome-panel">
          <div class="welcome-copy">
            <span class="welcome-date">{{ currentDateText }}</span>
            <h1>{{ greeting }}，{{ profile.name || '居民朋友' }}</h1>
            <p>今天也要记得关注身体变化。定期记录健康指标，让家庭医生更了解您。</p>
            <div class="welcome-actions">
              <el-button type="primary" size="large" :icon="Plus" @click="openAppointmentDialog">预约社区门诊</el-button>
              <el-button size="large" :icon="TrendCharts" @click="openHealthDialog">上报健康指标</el-button>
            </div>
          </div>
          <div class="health-score-card">
            <span class="latest-record-mark"><el-icon><DataLine /></el-icon></span>
            <div>
              <b>{{ latestHealth ? '最近一次健康记录' : '等待首次健康记录' }}</b>
              <p>{{ latestHealth ? `记录于 ${formatRecordTime(latestHealth.recordedAt)}` : '上报后可查看连续变化趋势' }}</p>
              <small v-if="latestHealth">居民自报数据，尚未经医护人员核验</small>
            </div>
          </div>
          <span class="welcome-orb welcome-orb--one" />
          <span class="welcome-orb welcome-orb--two" />
        </section>

        <section class="summary-strip">
          <div><span class="summary-icon mint"><el-icon><Calendar /></el-icon></span><p><b>{{ activeAppointments.length }}</b><span>待就诊预约</span></p></div>
          <div><span class="summary-icon lilac"><el-icon><CircleCheck /></el-icon></span><p><b>{{ sortedAppointments.filter(item => item.status === 'COMPLETED').length }}</b><span>已完成预约</span></p></div>
          <div><span class="summary-icon coral"><el-icon><Memo /></el-icon></span><p><b>{{ activePlanCount }}</b><span>进行中健康计划</span></p></div>
          <div><span class="summary-icon sky"><el-icon><DataLine /></el-icon></span><p><b>{{ healthRecords.length }}</b><span>健康记录</span></p></div>
        </section>

        <div class="section-heading">
          <div><span class="section-kicker">HEALTH SNAPSHOT</span><h2>我的健康概览</h2><p>最近一次上报的核心健康指标</p></div>
          <button class="text-action" type="button" @click="goTo('health')">查看全部记录 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <section class="indicator-grid">
          <article v-for="indicator in healthIndicators" :key="indicator.key" class="indicator-card">
            <div class="indicator-top">
              <span class="indicator-icon" :style="{ color: indicator.tint, background: `${indicator.tint}14` }"><el-icon><component :is="indicator.icon" /></el-icon></span>
              <span class="indicator-state" :class="indicator.state.tone">{{ indicator.state.label }}</span>
            </div>
            <p>{{ indicator.label }}</p>
            <strong>{{ indicator.value }} <small>{{ indicator.unit }}</small></strong>
            <span class="indicator-time">{{ latestHealth ? formatRecordTime(latestHealth.recordedAt) : '尚未上报' }}</span>
          </article>
        </section>

        <section class="overview-grid">
          <article class="content-card appointment-preview">
            <div class="card-heading"><div><span class="card-icon mint"><el-icon><Calendar /></el-icon></span><div><h3>下一次预约</h3><p>按时到诊，建议提前 10 分钟签到</p></div></div><button type="button" @click="goTo('appointments')">全部预约</button></div>
            <div v-if="upcomingAppointment" class="next-appointment">
              <div class="date-block"><strong>{{ parseDate(upcomingAppointment.scheduledAt)?.getDate() }}</strong><span>{{ (parseDate(upcomingAppointment.scheduledAt)?.getMonth() ?? 0) + 1 }} 月</span></div>
              <div class="appointment-person">
                <span class="doctor-avatar"><el-icon><FirstAidKit /></el-icon></span>
                <div><h4>{{ getDoctorName(upcomingAppointment) }}</h4><p>{{ upcomingAppointment.department || '社区门诊' }} · {{ upcomingAppointment.reason }}</p><span><el-icon><Clock /></el-icon>{{ formatDateTime(upcomingAppointment.scheduledAt) }}</span></div>
              </div>
              <span class="status-pill" :class="statusMeta[upcomingAppointment.status].tone">{{ statusMeta[upcomingAppointment.status].label }}</span>
            </div>
            <div v-else class="empty-inline"><span><el-icon><Calendar /></el-icon></span><div><b>近期没有预约</b><p>身体不适或需要复诊时，可在线预约社区医生。</p></div><el-button type="primary" plain @click="openAppointmentDialog">立即预约</el-button></div>
          </article>

          <article class="content-card plan-preview">
            <div class="card-heading"><div><span class="card-icon coral"><el-icon><Memo /></el-icon></span><div><h3>今日健康行动</h3><p>小行动，积累长期健康</p></div></div><button type="button" @click="goTo('chronic')">我的计划</button></div>
            <div class="daily-actions">
              <div><span class="check-dot" :class="{ done: hasHealthRecordToday }"><el-icon v-if="hasHealthRecordToday"><CircleCheck /></el-icon><i v-else /></span><p><b>记录今日血压</b><small>{{ hasHealthRecordToday ? '今日指标已记录' : '建议按医护人员指导测量' }}</small></p><span>{{ hasHealthRecordToday ? '已记录' : '待记录' }}</span></div>
              <div><span class="check-dot"><i /></span><p><b>适量有氧运动</b><small>建议慢走或健步 30 分钟</small></p><span>30 分钟</span></div>
              <div><span class="check-dot"><i /></span><p><b>按计划服药</b><small>请遵循医生处方，不自行停药</small></p><span>查看计划</span></div>
            </div>
          </article>
        </section>

        <section class="care-tip">
          <span><el-icon><FirstAidKit /></el-icon></span>
          <div><b>家庭医生温馨提醒</b><p>如连续出现血压明显升高、胸痛或呼吸困难，请及时联系家庭医生；紧急情况请立即拨打 {{ portalConfig.emergencyPhone }}。</p></div>
          <el-button text @click="goTo('chronic')">查看健康计划 <el-icon><ArrowRight /></el-icon></el-button>
        </section>
      </template>

      <template v-else-if="activeSection === 'appointments'">
        <section class="subpage-hero appointments-hero">
          <div><span class="section-kicker">APPOINTMENTS</span><h1>我的预约</h1><p>在线预约社区门诊，随时了解预约进度</p></div>
          <el-button type="primary" size="large" :icon="Plus" @click="openAppointmentDialog">预约门诊</el-button>
        </section>
        <section class="appointment-stats">
          <div><span class="summary-icon mint"><el-icon><Clock /></el-icon></span><p><b>{{ sortedAppointments.filter(item => item.status === 'PENDING').length }}</b><span>待确认</span></p></div>
          <div><span class="summary-icon sky"><el-icon><Calendar /></el-icon></span><p><b>{{ sortedAppointments.filter(item => item.status === 'CONFIRMED').length }}</b><span>待就诊</span></p></div>
          <div><span class="summary-icon lilac"><el-icon><CircleCheck /></el-icon></span><p><b>{{ sortedAppointments.filter(item => item.status === 'COMPLETED').length }}</b><span>已完成</span></p></div>
        </section>
        <section class="content-card appointment-list-card">
          <div class="card-heading"><div><span class="card-icon mint"><el-icon><Calendar /></el-icon></span><div><h3>预约记录</h3><p>共 {{ sortedAppointments.length }} 条预约</p></div></div></div>
          <div v-if="sortedAppointments.length" class="appointment-list">
            <article v-for="item in sortedAppointments" :key="item.id" class="appointment-row">
              <div class="date-block"><strong>{{ parseDate(item.scheduledAt)?.getDate() }}</strong><span>{{ (parseDate(item.scheduledAt)?.getMonth() ?? 0) + 1 }} 月</span></div>
              <span class="doctor-avatar"><el-icon><FirstAidKit /></el-icon></span>
              <div class="appointment-main"><div><h4>{{ getDoctorName(item) }}</h4><span class="status-pill" :class="statusMeta[item.status].tone">{{ statusMeta[item.status].label }}</span></div><p>{{ item.department || '社区门诊' }} · {{ item.reason }}</p><span><el-icon><Clock /></el-icon>{{ formatDateTime(item.scheduledAt) }}<i>·</i>预约号 {{ item.appointmentNo || '--' }}</span></div>
              <el-button v-if="item.status === 'PENDING' || item.status === 'CONFIRMED'" type="danger" plain @click="cancelAppointment(item)">取消预约</el-button>
            </article>
          </div>
          <div v-else-if="moduleStates.appointments !== 'error'" class="empty-state"><span><el-icon><Calendar /></el-icon></span><h3>还没有预约记录</h3><p>选择合适的社区医生和时间，即可在线提交预约。</p><el-button type="primary" @click="openAppointmentDialog">立即预约</el-button></div>
          <div v-else class="empty-state compact"><span><el-icon><Warning /></el-icon></span><h3>预约记录加载失败</h3><p>请刷新页面或稍后重试。</p></div>
        </section>
      </template>

      <template v-else-if="activeSection === 'health'">
        <section class="subpage-hero health-hero">
          <div><span class="section-kicker">HEALTH MONITORING</span><h1>健康监测</h1><p>持续记录，让身体的每一点变化都有迹可循</p></div>
          <el-button type="primary" size="large" :icon="Plus" @click="openHealthDialog">上报指标</el-button>
        </section>
        <section class="indicator-grid health-page-indicators">
          <article v-for="indicator in healthIndicators" :key="indicator.key" class="indicator-card">
            <div class="indicator-top"><span class="indicator-icon" :style="{ color: indicator.tint, background: `${indicator.tint}14` }"><el-icon><component :is="indicator.icon" /></el-icon></span><span class="indicator-state" :class="indicator.state.tone">{{ indicator.state.label }}</span></div>
            <p>{{ indicator.label }}</p><strong>{{ indicator.value }} <small>{{ indicator.unit }}</small></strong><span class="indicator-time">最新记录</span>
          </article>
        </section>
        <section class="health-layout">
          <article class="content-card trend-card">
            <div class="card-heading"><div><span class="card-icon sky"><el-icon><DataLine /></el-icon></span><div><h3>近期血压趋势</h3><p>最近 7 次上报结果</p></div></div><div class="chart-legend"><span><i class="systolic" />收缩压</span><span><i class="diastolic" />舒张压</span></div></div>
            <div v-if="chronologicalRecords.length" class="trend-chart">
              <svg viewBox="0 0 360 125" role="img" aria-label="近期血压趋势图">
                <line v-for="y in [24, 51, 78, 105]" :key="y" x1="16" :y1="y" x2="344" :y2="y" class="grid-line" />
                <polyline v-for="(points, index) in trendSegments('systolicPressure')" :key="`s-${index}`" :points="points" class="trend-line systolic-line" />
                <polyline v-for="(points, index) in trendSegments('diastolicPressure')" :key="`d-${index}`" :points="points" class="trend-line diastolic-line" />
                <circle v-for="point in trendPoints('systolicPressure')" :key="`sp-${point.index}`" :cx="point.x" :cy="point.y" r="3" class="trend-point systolic-point" />
                <circle v-for="point in trendPoints('diastolicPressure')" :key="`dp-${point.index}`" :cx="point.x" :cy="point.y" r="3" class="trend-point diastolic-point" />
              </svg>
              <div class="trend-labels"><span v-for="record in chronologicalRecords" :key="record.id">{{ dayLabel(record.recordedAt) }}</span></div>
            </div>
            <div v-else class="chart-empty"><el-icon><DataLine /></el-icon><span>上报至少一次血压后显示趋势</span></div>
          </article>
          <aside class="content-card monitor-guide">
            <span class="card-icon coral"><el-icon><FirstAidKit /></el-icon></span><h3>家庭测量小贴士</h3>
            <ul><li>测量前安静休息 5 分钟</li><li>手臂与心脏保持同一高度</li><li>固定时间、同侧手臂连续记录</li><li>数据异常时复测并联系医生</li></ul>
            <button type="button" @click="openHealthDialog">记录本次测量 <el-icon><ArrowRight /></el-icon></button>
          </aside>
        </section>
        <section class="content-card records-card">
          <div class="card-heading"><div><span class="card-icon lilac"><el-icon><Memo /></el-icon></span><div><h3>历史上报记录</h3><p>最近 {{ recentHealthRecords.length }} 条健康指标</p></div></div></div>
          <div v-if="recentHealthRecords.length" class="record-table-wrap">
            <table class="record-table"><thead><tr><th>记录时间</th><th>血压</th><th>心率</th><th>血氧</th><th>体重</th><th>备注</th></tr></thead><tbody><tr v-for="record in recentHealthRecords" :key="record.id"><td>{{ formatRecordTime(record.recordedAt) }}</td><td><b>{{ record.systolicPressure ?? '--' }}/{{ record.diastolicPressure ?? '--' }}</b> mmHg</td><td>{{ record.heartRate ?? '--' }} 次/分</td><td>{{ record.bloodOxygen ?? '--' }}%</td><td>{{ record.weight ?? '--' }} kg</td><td>{{ record.note || '—' }}</td></tr></tbody></table>
          </div>
          <div v-else-if="moduleStates.health !== 'error'" class="empty-state compact"><span><el-icon><TrendCharts /></el-icon></span><h3>还没有健康记录</h3><p>记录血压等指标，建立连续的个人健康趋势。</p><el-button type="primary" @click="openHealthDialog">首次上报</el-button></div>
          <div v-else class="empty-state compact"><span><el-icon><Warning /></el-icon></span><h3>健康记录加载失败</h3><p>请刷新页面或稍后重试。</p></div>
        </section>
      </template>

      <template v-else-if="activeSection === 'chronic'">
        <section class="subpage-hero chronic-hero"><div><span class="section-kicker">CARE PLAN</span><h1>我的慢病计划</h1><p>与家庭医生一起管理慢病，按计划完成每一个健康目标</p></div><span class="hero-illustration"><el-icon><Memo /></el-icon></span></section>
        <section v-if="chronicPlans.length" class="plan-list">
          <article v-for="(plan, index) in chronicPlans" :key="plan.id" class="content-card chronic-card">
            <div class="chronic-card__header">
              <div><span class="disease-mark" :class="index % 2 ? 'purple' : 'green'"><el-icon><FirstAidKit /></el-icon></span><div><span class="plan-state"><i />{{ plan.active === false ? '计划已结束' : '管理中' }}</span><h2>{{ plan.diseaseType }}</h2><p v-if="plan.diagnosisDate">建档于 {{ formatDate(plan.diagnosisDate) }}</p></div></div>
              <span class="risk-badge" :class="plan.riskLevel?.includes('高') ? 'high' : ''">{{ plan.riskLevel || '未分级' }}</span>
            </div>
            <div class="plan-details"><div><span><el-icon><User /></el-icon>责任医生</span><b>{{ getPlanDoctor(plan) }}</b></div><div><span><el-icon><Calendar /></el-icon>下次随访</span><b>{{ plan.nextFollowUp ? formatDate(plan.nextFollowUp) : '等待医生安排' }}</b></div></div>
            <div class="management-plan"><span>医生制定的管理方案</span><p>{{ plan.managementPlan || '暂无医生录入的管理方案' }}</p></div>
            <div class="plan-progress pending"><span>履约进度将在医护服务任务上线后显示</span></div>
          </article>
        </section>
        <section v-else-if="moduleStates.chronic !== 'error'" class="content-card empty-state large"><span><el-icon><CircleCheck /></el-icon></span><h2>当前没有慢病管理计划</h2><p>您的健康档案中暂无在管慢病。如有长期用药或慢性疾病，请联系家庭医生建档。</p></section>
        <section v-else class="content-card empty-state large"><span><el-icon><Warning /></el-icon></span><h2>慢病计划加载失败</h2><p>请稍后重试；已建档计划不会因此被删除。</p></section>
        <section class="care-tip"><span><el-icon><Phone /></el-icon></span><div><b>需要帮助？联系家庭医生</b><p>服务时间：{{ portalConfig.serviceHours || '请咨询社区服务中心' }}</p></div><el-button v-if="portalConfig.servicePhone" text tag="a" :href="`tel:${portalConfig.servicePhone}`">社区服务电话 {{ portalConfig.servicePhone }}</el-button><small v-else class="service-unavailable">服务电话待配置</small></section>
      </template>

      <template v-else>
        <section class="subpage-hero profile-hero"><div><span class="section-kicker">PERSONAL PROFILE</span><h1>个人健康档案</h1><p>保持联系方式准确，以便社区医生及时为您服务</p></div><span class="profile-hero-avatar">{{ residentFirstName }}</span></section>
        <section class="profile-layout">
          <aside class="content-card profile-summary">
            <span class="profile-avatar-large">{{ residentFirstName }}</span><h2>{{ profile.name || '居民用户' }}</h2><p>社区健康档案居民</p>
            <div class="profile-tags"><span>{{ profile.gender || '性别未填写' }}</span><span>{{ profile.bloodType ? `${profile.bloodType} 型血` : '血型未登记' }}</span></div>
            <dl><div><dt>档案编号</dt><dd>{{ profile.id ? `R${String(profile.id).padStart(8, '0')}` : '待同步' }}</dd></div><div><dt>出生日期</dt><dd>{{ profile.birthDate ? formatDate(profile.birthDate) : '未登记' }}</dd></div><div><dt>证件号码</dt><dd>{{ profile.idCard ? `${profile.idCard.slice(0, 4)}**********${profile.idCard.slice(-4)}` : '未登记' }}</dd></div></dl>
          </aside>
          <article class="content-card profile-form-card">
            <div class="card-heading"><div><span class="card-icon mint"><el-icon><EditPen /></el-icon></span><div><h3>联系方式</h3><p>修改后将同步至您的社区健康档案</p></div></div></div>
            <el-form ref="profileFormRef" :model="profileDraft" :rules="profileRules" label-position="top" class="profile-form">
              <el-form-item label="姓名"><el-input :model-value="profile.name" disabled><template #prefix><el-icon><User /></el-icon></template></el-input><span class="form-hint">姓名及证件信息如需修改，请携带证件到社区服务中心办理。</span></el-form-item>
              <el-form-item label="手机号码" prop="phone"><el-input v-model="profileDraft.phone" maxlength="11"><template #prefix><el-icon><Phone /></el-icon></template></el-input></el-form-item>
              <el-form-item label="当前居住地址" prop="address"><el-input v-model="profileDraft.address" type="textarea" :rows="3" maxlength="200" show-word-limit><template #prefix><el-icon><Location /></el-icon></template></el-input></el-form-item>
              <div class="profile-form-actions"><el-button @click="profileDraft.phone = profile.phone; profileDraft.address = profile.address">恢复</el-button><el-button type="primary" :loading="profileSaving" @click="saveProfile">保存修改</el-button></div>
            </el-form>
          </article>
        </section>
        <section class="profile-notice"><el-icon><Warning /></el-icon><div><b>隐私与数据安全</b><p>您的健康数据仅用于社区健康管理与医疗服务。系统采用身份认证和访问控制保护个人信息。</p></div></section>
      </template>
    </main>

    <div class="mobile-nav" aria-label="居民门户移动导航">
      <button v-for="item in navItems" :key="item.key" type="button" :class="{ active: activeSection === item.key }" @click="goTo(item.key)"><el-icon><component :is="item.icon" /></el-icon><span>{{ item.label.replace('健康', '') || '健康' }}</span></button>
    </div>

    <el-dialog v-model="appointmentDialogVisible" title="预约社区门诊" width="560px" class="portal-dialog" destroy-on-close>
      <div class="dialog-intro"><span class="card-icon mint"><el-icon><Calendar /></el-icon></span><div><b>选择方便的就诊时间</b><p>提交后由社区医护人员确认，请留意预约状态。</p></div></div>
      <el-form ref="appointmentFormRef" :model="appointmentForm" :rules="appointmentRules" label-position="top">
        <el-form-item label="接诊医生" prop="doctorId"><el-select v-model="appointmentForm.doctorId" placeholder="请选择医生" filterable style="width: 100%"><el-option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id" :label="`${doctor.name} · ${doctor.department || doctor.specialty || '社区门诊'}`"><div class="doctor-option"><span>{{ doctor.name }}<small>{{ doctor.title || '社区医生' }}</small></span><i>{{ doctor.department || doctor.specialty || '社区门诊' }}</i></div></el-option></el-select></el-form-item>
        <el-form-item label="预约时间" prop="scheduledAt"><el-date-picker v-model="appointmentForm.scheduledAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY年MM月DD日 HH:mm" placeholder="请选择日期和时间" style="width: 100%" /></el-form-item>
        <el-form-item label="就诊原因" prop="reason"><el-input v-model="appointmentForm.reason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="例如：近期血压偏高，希望复诊调整用药" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="appointmentDialogVisible = false">暂不预约</el-button><el-button type="primary" :loading="appointmentSaving" @click="submitAppointment">提交预约</el-button></template>
    </el-dialog>

    <el-dialog v-model="healthDialogVisible" title="上报健康指标" width="620px" class="portal-dialog" destroy-on-close>
      <div class="dialog-intro"><span class="card-icon coral"><el-icon><TrendCharts /></el-icon></span><div><b>记录本次居家测量</b><p>请填写真实测量结果，血压为必填项。</p></div></div>
      <el-form ref="healthFormRef" :model="healthForm" :rules="healthRules" label-position="top">
        <div class="metric-form-grid">
          <el-form-item label="收缩压（高压）" prop="systolicPressure"><el-input-number v-model="healthForm.systolicPressure" :min="40" :max="260" :step="1" controls-position="right" /><span class="input-unit">mmHg</span></el-form-item>
          <el-form-item label="舒张压（低压）" prop="diastolicPressure"><el-input-number v-model="healthForm.diastolicPressure" :min="30" :max="180" :step="1" controls-position="right" /><span class="input-unit">mmHg</span></el-form-item>
          <el-form-item label="静息心率"><el-input-number v-model="healthForm.heartRate" :min="20" :max="250" :step="1" controls-position="right" /><span class="input-unit">次/分</span></el-form-item>
          <el-form-item label="血氧饱和度"><el-input-number v-model="healthForm.bloodOxygen" :min="50" :max="100" :step="1" controls-position="right" /><span class="input-unit">%</span></el-form-item>
          <el-form-item label="体重"><el-input-number v-model="healthForm.weight" :min="20" :max="250" :precision="1" :step="0.1" controls-position="right" /><span class="input-unit">kg</span></el-form-item>
          <el-form-item label="备注" class="metric-note"><el-input v-model="healthForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可填写测量时的身体感受或用药情况" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="healthDialogVisible = false">取消</el-button><el-button type="primary" :loading="healthSaving" @click="submitHealthRecord">确认上报</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
:global(body:has(.resident-portal)){min-width:320px;background:#f4f8f7}.resident-portal{--portal-green:#0c7568;--portal-dark:#173c38;--portal-muted:#718d89;--portal-line:#e2ece9;min-height:100vh;color:var(--portal-dark);background:radial-gradient(circle at 95% 8%,rgba(179,229,218,.3),transparent 24%),#f4f8f7}.resident-portal button{font:inherit}.portal-header{position:sticky;top:0;z-index:30;border-bottom:1px solid rgba(221,235,231,.9);background:rgba(255,255,255,.9);backdrop-filter:blur(18px)}.portal-header__inner{width:min(1180px,calc(100% - 48px));height:78px;margin:auto;display:flex;align-items:center;justify-content:space-between;gap:28px}.portal-brand{padding:0;display:flex;align-items:center;gap:11px;border:0;color:var(--portal-dark);background:none;cursor:pointer;text-align:left}.portal-brand__mark{width:40px;height:40px;display:grid;place-items:center;border-radius:13px;color:white;background:linear-gradient(145deg,#18a08c,#09665d);box-shadow:0 8px 20px rgba(12,117,104,.23);font-size:20px}.portal-brand b,.portal-brand small{display:block;white-space:nowrap}.portal-brand b{font-size:17px;letter-spacing:.03em}.portal-brand small{margin-top:2px;color:#8aa09d;font-size:9px;letter-spacing:.12em}.portal-nav{display:flex;align-self:stretch;gap:4px}.portal-nav button{position:relative;padding:0 15px;display:flex;align-items:center;gap:7px;border:0;color:#728b87;background:transparent;cursor:pointer;font-size:13px}.portal-nav button:after{content:"";position:absolute;right:15px;bottom:0;left:15px;height:3px;border-radius:3px 3px 0 0;background:var(--portal-green);transform:scaleX(0);transition:.2s}.portal-nav button:hover,.portal-nav button.active{color:var(--portal-green)}.portal-nav button.active:after{transform:scaleX(1)}.portal-actions{display:flex;align-items:center;gap:8px}.round-action{position:relative;width:38px;height:38px;display:grid;place-items:center;border:1px solid #e4eeeb;border-radius:12px;color:#69837f;background:#f8fbfa;cursor:pointer}.notification i{position:absolute;right:8px;top:7px;width:6px;height:6px;border-radius:50%;background:#ec6572;box-shadow:0 0 0 2px white}.resident-chip{padding:3px 3px 3px 7px;margin-left:4px;display:flex;align-items:center;gap:9px;border:0;color:var(--portal-dark);background:transparent;cursor:pointer;text-align:left}.resident-avatar{width:36px;height:36px;display:grid;place-items:center;border-radius:12px;color:#087267;background:#daf1eb;font-weight:800}.resident-chip b,.resident-chip small{display:block;white-space:nowrap}.resident-chip b{font-size:13px}.resident-chip small{margin-top:2px;color:#8ca09e;font-size:10px}.portal-main{width:min(1120px,calc(100% - 48px));margin:0 auto;padding:34px 0 58px}.welcome-panel{position:relative;min-height:265px;padding:42px 46px;display:flex;align-items:center;justify-content:space-between;overflow:hidden;border-radius:28px;color:white;background:linear-gradient(125deg,#086d65,#138d79 58%,#30a786);box-shadow:0 20px 45px rgba(7,100,89,.18)}.welcome-copy{position:relative;z-index:2;max-width:590px}.welcome-date{font-size:13px;color:#c8ebe4}.welcome-copy h1{margin:10px 0 12px;font-size:34px;line-height:1.2;letter-spacing:-.02em}.welcome-copy p{margin:0;color:#d2eee9;font-size:14px;line-height:1.8}.welcome-actions{margin-top:27px;display:flex;gap:10px}.welcome-actions :deep(.el-button--primary){--el-button-bg-color:white;--el-button-border-color:white;--el-button-text-color:#087166;--el-button-hover-bg-color:#effbf8;--el-button-hover-border-color:#effbf8}.welcome-actions :deep(.el-button:not(.el-button--primary)){--el-button-bg-color:rgba(255,255,255,.1);--el-button-border-color:rgba(255,255,255,.35);--el-button-text-color:white;--el-button-hover-bg-color:rgba(255,255,255,.18);--el-button-hover-border-color:rgba(255,255,255,.5)}.health-score-card{position:relative;z-index:2;width:285px;padding:23px;display:flex;align-items:center;gap:17px;border:1px solid rgba(255,255,255,.22);border-radius:21px;background:rgba(255,255,255,.12);backdrop-filter:blur(12px)}.score-ring{--score:0deg;width:93px;height:93px;flex:none;padding:7px;border-radius:50%;background:conic-gradient(#d6ffcc var(--score),rgba(255,255,255,.2) 0)}.score-ring>div{height:100%;display:grid;place-content:center;text-align:center;border-radius:50%;background:#158777}.score-ring strong,.score-ring span{display:block}.score-ring strong{font-size:27px}.score-ring span{color:#d8f2ed;font-size:9px}.health-score-card>div:last-child b{font-size:15px}.health-score-card>div:last-child p{margin:7px 0 0;color:#cbe8e2;font-size:10px;line-height:1.5}.welcome-orb{position:absolute;border-radius:50%;border:1px solid rgba(255,255,255,.12)}.welcome-orb--one{width:320px;height:320px;right:-95px;top:-155px}.welcome-orb--two{width:190px;height:190px;right:260px;bottom:-140px;background:rgba(126,225,182,.09)}.summary-strip{position:relative;z-index:3;width:calc(100% - 60px);margin:-19px auto 31px;padding:18px 24px;display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #e2ece9;border-radius:19px;background:white;box-shadow:0 12px 36px rgba(24,76,70,.08)}.summary-strip>div,.appointment-stats>div{padding:3px 20px;display:flex;align-items:center;gap:13px;border-right:1px solid #e9f0ee}.summary-strip>div:last-child,.appointment-stats>div:last-child{border-right:0}.summary-icon,.card-icon{display:grid;place-items:center;flex:none;border-radius:12px}.summary-icon{width:42px;height:42px;font-size:18px}.summary-icon.mint,.card-icon.mint{color:#0d8273;background:#dff4ef}.summary-icon.lilac,.card-icon.lilac{color:#7262c6;background:#eeebff}.summary-icon.coral,.card-icon.coral{color:#d75d6d;background:#ffe9eb}.summary-icon.sky,.card-icon.sky{color:#3185bc;background:#e5f3fb}.summary-strip p,.appointment-stats p{margin:0}.summary-strip b,.summary-strip span,.appointment-stats b,.appointment-stats span{display:block}.summary-strip b,.appointment-stats b{font-size:19px}.summary-strip p>span,.appointment-stats p>span{margin-top:2px;color:#809592;font-size:10px}.section-heading{margin:0 2px 17px;display:flex;align-items:end;justify-content:space-between}.section-kicker{color:#20a18e;font-size:9px;font-weight:800;letter-spacing:.18em}.section-heading h2{margin:5px 0 3px;font-size:23px}.section-heading p{margin:0;color:var(--portal-muted);font-size:12px}.text-action{display:flex;align-items:center;gap:6px;border:0;color:var(--portal-green);background:transparent;cursor:pointer;font-size:12px}.indicator-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.indicator-card{padding:20px;border:1px solid var(--portal-line);border-radius:19px;background:white;box-shadow:0 7px 26px rgba(30,77,72,.05);transition:transform .2s,box-shadow .2s}.indicator-card:hover{transform:translateY(-2px);box-shadow:0 12px 30px rgba(30,77,72,.09)}.indicator-top{display:flex;align-items:center;justify-content:space-between}.indicator-icon{width:39px;height:39px;display:grid;place-items:center;border-radius:12px;font-size:18px}.indicator-state{padding:4px 8px;border-radius:20px;font-size:9px}.indicator-state.good{color:#107860;background:#e0f5ed}.indicator-state.warning{color:#a76619;background:#fff1d9}.indicator-state.danger{color:#c74857;background:#ffe5e8}.indicator-state.neutral{color:#758885;background:#edf2f1}.indicator-card>p{margin:17px 0 4px;color:#778d89;font-size:11px}.indicator-card>strong{display:block;font-size:23px}.indicator-card>strong small{color:#829691;font-size:10px;font-weight:500}.indicator-time{display:block;margin-top:9px;color:#a0afad;font-size:9px}.overview-grid{margin-top:19px;display:grid;grid-template-columns:1.08fr .92fr;gap:16px}.content-card{border:1px solid var(--portal-line);border-radius:21px;background:white;box-shadow:0 9px 30px rgba(30,77,72,.05)}.appointment-preview,.plan-preview,.appointment-list-card,.trend-card,.monitor-guide,.records-card,.profile-form-card,.profile-summary,.chronic-card{padding:22px}.card-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.card-heading>div:first-child{display:flex;align-items:center;gap:11px}.card-icon{width:39px;height:39px;font-size:17px}.card-heading h3,.card-heading p{margin:0}.card-heading h3{font-size:15px}.card-heading p{margin-top:4px;color:#8ba09c;font-size:9px}.card-heading>button{border:0;color:var(--portal-green);background:transparent;cursor:pointer;font-size:11px}.next-appointment{margin-top:21px;padding:18px;display:flex;align-items:center;gap:15px;border-radius:16px;background:#f5faf8}.date-block{width:52px;height:57px;display:grid;place-content:center;text-align:center;border-radius:13px;color:#0b7166;background:#dff3ed;flex:none}.date-block strong,.date-block span{display:block}.date-block strong{font-size:23px;line-height:1}.date-block span{margin-top:4px;font-size:9px}.appointment-person{min-width:0;flex:1;display:flex;align-items:center;gap:12px}.doctor-avatar{width:42px;height:42px;display:grid;place-items:center;flex:none;border-radius:50%;color:#5b776f;background:white;border:1px solid #e1ece9;font-size:18px}.appointment-person h4,.appointment-person p{margin:0}.appointment-person h4{font-size:13px}.appointment-person p{margin-top:3px;overflow:hidden;color:#7d918e;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.appointment-person>div>span,.appointment-main>span{margin-top:8px;display:flex;align-items:center;gap:5px;color:#506f6b;font-size:10px}.status-pill{padding:5px 9px;border-radius:20px;font-size:9px;white-space:nowrap}.status-pill.green{color:#11745f;background:#ddf4eb}.status-pill.amber{color:#a96718;background:#fff0d4}.status-pill.blue{color:#307cb1;background:#e5f2fa}.status-pill.gray{color:#7f908e;background:#edf1f0}.empty-inline{margin-top:19px;padding:17px;display:flex;align-items:center;gap:13px;border-radius:15px;background:#f6faf9}.empty-inline>span{width:40px;height:40px;display:grid;place-items:center;border-radius:12px;color:#78938e;background:white}.empty-inline>div{min-width:0;flex:1}.empty-inline b,.empty-inline p{display:block;margin:0}.empty-inline b{font-size:12px}.empty-inline p{margin-top:3px;color:#8ca09d;font-size:9px}.daily-actions{margin-top:15px;display:grid}.daily-actions>div{padding:11px 2px;display:flex;align-items:center;gap:10px;border-bottom:1px solid #edf2f1}.daily-actions>div:last-child{border-bottom:0}.check-dot{width:25px;height:25px;display:grid;place-items:center;flex:none;border-radius:50%;color:#9ab0ac;background:#f0f4f3}.check-dot.done{color:white;background:#1b9a80}.check-dot i{width:7px;height:7px;border-radius:50%;background:#adc0bd}.daily-actions p{min-width:0;flex:1;margin:0}.daily-actions b,.daily-actions small{display:block}.daily-actions b{font-size:11px}.daily-actions small{margin-top:3px;color:#8ca09d;font-size:8px}.daily-actions>div>span:last-child{color:#75908b;font-size:9px}.care-tip{margin-top:18px;padding:17px 21px;display:flex;align-items:center;gap:14px;border:1px solid #dceee9;border-radius:17px;background:linear-gradient(90deg,#edf9f6,#f8fbfa)}.care-tip>span{width:40px;height:40px;display:grid;place-items:center;flex:none;border-radius:12px;color:#0b7b6d;background:white;font-size:18px}.care-tip>div{min-width:0;flex:1}.care-tip b,.care-tip p{display:block;margin:0}.care-tip b{font-size:12px}.care-tip p{margin-top:4px;color:#718b87;font-size:10px;line-height:1.5}.care-tip :deep(.el-button){color:var(--portal-green);font-size:11px}.subpage-hero{min-height:165px;margin-bottom:18px;padding:31px 35px;display:flex;align-items:center;justify-content:space-between;overflow:hidden;border:1px solid #dcece8;border-radius:25px;background:linear-gradient(120deg,#effaf7,#f9fcfb)}.subpage-hero h1{margin:6px 0 8px;font-size:29px}.subpage-hero p{margin:0;color:#6e8985;font-size:12px}.appointments-hero{background:radial-gradient(circle at 82% 15%,#d9f2ec,transparent 31%),linear-gradient(120deg,#effaf7,#f9fcfb)}.health-hero{background:radial-gradient(circle at 85% 20%,#e2edff,transparent 30%),linear-gradient(120deg,#f0f8fb,#fbfcfd)}.chronic-hero{background:radial-gradient(circle at 84% 10%,#e4dafb,transparent 29%),linear-gradient(120deg,#f5f1fc,#fbfafc)}.profile-hero{background:radial-gradient(circle at 84% 10%,#d4efe7,transparent 29%),linear-gradient(120deg,#eff9f6,#fbfcfc)}.hero-illustration,.profile-hero-avatar{width:86px;height:86px;margin-right:40px;display:grid;place-items:center;border-radius:26px;transform:rotate(5deg);color:#705bb0;background:rgba(255,255,255,.68);box-shadow:0 12px 28px rgba(79,58,121,.12);font-size:38px}.profile-hero-avatar{color:#0b776a;background:#d9f1ea;font-size:28px;font-weight:800}.appointment-stats{margin-bottom:18px;padding:18px 24px;display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--portal-line);border-radius:19px;background:white}.appointment-stats>div{justify-content:center}.appointment-list-card{padding-bottom:8px}.appointment-list{margin-top:15px}.appointment-row{padding:18px 4px;display:flex;align-items:center;gap:16px;border-top:1px solid #edf2f1}.appointment-row:first-child{border-top:0}.appointment-main{min-width:0;flex:1}.appointment-main>div{display:flex;align-items:center;gap:9px}.appointment-main h4,.appointment-main p{margin:0}.appointment-main h4{font-size:14px}.appointment-main p{margin-top:5px;color:#758d89;font-size:11px}.appointment-main>span i{font-style:normal;color:#b2bfbd}.empty-state{min-height:290px;display:grid;place-items:center;align-content:center;text-align:center}.empty-state>span{width:63px;height:63px;display:grid;place-items:center;border-radius:20px;color:#0d8474;background:#e2f5ef;font-size:27px}.empty-state h3,.empty-state h2{margin:14px 0 5px}.empty-state p{max-width:430px;margin:0 0 18px;color:#809692;font-size:11px;line-height:1.7}.empty-state.compact{min-height:230px}.empty-state.large{min-height:360px}.health-page-indicators{margin-bottom:18px}.health-layout{display:grid;grid-template-columns:minmax(0,1fr) 285px;gap:16px}.trend-card{min-width:0}.chart-legend{display:flex;gap:12px;color:#718985;font-size:9px}.chart-legend span{display:flex;align-items:center;gap:5px}.chart-legend i{width:14px;height:3px;border-radius:3px}.chart-legend i.systolic{background:#e95b73}.chart-legend i.diastolic{background:#4e8fd2}.trend-chart{margin-top:21px;padding:3px 5px 0;border-radius:14px;background:linear-gradient(180deg,#fbfdfd,#f7faf9)}.trend-chart svg{width:100%;height:170px;overflow:visible}.grid-line{stroke:#e8efed;stroke-width:1}.trend-line{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.systolic-line{stroke:#e95b73}.diastolic-line{stroke:#4e8fd2}.trend-labels{padding:0 9px 4px;display:flex;justify-content:space-between;color:#8ba09c;font-size:9px}.chart-empty{height:190px;display:grid;place-content:center;gap:8px;text-align:center;color:#8aa09c;font-size:10px}.chart-empty .el-icon{margin:auto;font-size:32px;color:#b9cac7}.monitor-guide{background:linear-gradient(155deg,#0b7268,#15907b);color:white}.monitor-guide>.card-icon{color:#0d806f;background:#d9f3ec}.monitor-guide h3{margin:18px 0 15px;font-size:17px}.monitor-guide ul{margin:0;padding:0;list-style:none}.monitor-guide li{position:relative;padding:9px 0 9px 18px;border-bottom:1px solid rgba(255,255,255,.12);color:#d0ece7;font-size:10px}.monitor-guide li:before{content:"";position:absolute;left:2px;top:14px;width:6px;height:6px;border-radius:50%;background:#9fe1ca}.monitor-guide button{margin-top:18px;padding:0;display:flex;align-items:center;gap:5px;border:0;color:white;background:none;cursor:pointer;font-size:10px}.records-card{margin-top:18px}.record-table-wrap{margin-top:18px;overflow-x:auto}.record-table{width:100%;border-collapse:collapse;font-size:10px}.record-table th{padding:11px 12px;color:#79918d;background:#f5f9f8;text-align:left;font-weight:600;white-space:nowrap}.record-table th:first-child{border-radius:10px 0 0 10px}.record-table th:last-child{border-radius:0 10px 10px 0}.record-table td{padding:15px 12px;border-bottom:1px solid #edf2f1;color:#58726e;white-space:nowrap}.record-table td b{color:#234741}.plan-list{display:grid;grid-template-columns:repeat(2,1fr);gap:17px}.chronic-card__header,.chronic-card__header>div:first-child{display:flex;align-items:center;justify-content:space-between;gap:13px}.disease-mark{width:55px;height:55px;display:grid;place-items:center;flex:none;border-radius:17px;color:#0d806f;background:#dff3ee;font-size:23px}.disease-mark.purple{color:#755dbb;background:#eee8ff}.plan-state{display:flex;align-items:center;gap:5px;color:#14816e;font-size:9px}.plan-state i{width:6px;height:6px;border-radius:50%;background:#1ba184}.chronic-card__header h2,.chronic-card__header p{margin:0}.chronic-card__header h2{margin-top:3px;font-size:19px}.chronic-card__header p{margin-top:3px;color:#8da19e;font-size:9px}.risk-badge{padding:5px 9px;border-radius:20px;color:#a46b23;background:#fff0d8;font-size:9px}.risk-badge.high{color:#c34e5c;background:#ffe4e7}.plan-details{margin-top:20px;padding:14px;display:grid;grid-template-columns:1fr 1fr;gap:10px;border-radius:14px;background:#f5f9f8}.plan-details>div{padding:0 8px;border-right:1px solid #e0eae8}.plan-details>div:last-child{border-right:0}.plan-details span,.plan-details b{display:flex;align-items:center;gap:5px}.plan-details span{color:#839995;font-size:9px}.plan-details b{margin-top:5px;font-size:11px}.management-plan{margin-top:17px}.management-plan span{color:#758c88;font-size:9px}.management-plan p{min-height:47px;margin:7px 0 0;color:#3f615c;font-size:11px;line-height:1.7}.plan-progress{margin-top:17px}.plan-progress>div{margin-bottom:7px;display:flex;justify-content:space-between;font-size:9px}.plan-progress>div span{color:#809591}.profile-layout{display:grid;grid-template-columns:330px minmax(0,1fr);gap:17px}.profile-summary{text-align:center}.profile-avatar-large{width:90px;height:90px;margin:5px auto 13px;display:grid;place-items:center;border-radius:28px;color:white;background:linear-gradient(145deg,#24a18d,#0a7166);box-shadow:0 13px 30px rgba(10,113,102,.2);font-size:30px;font-weight:800}.profile-summary h2,.profile-summary>p{margin:0}.profile-summary h2{font-size:20px}.profile-summary>p{margin-top:4px;color:#879d99;font-size:10px}.profile-tags{margin:15px 0;display:flex;justify-content:center;gap:7px}.profile-tags span{padding:5px 9px;border-radius:20px;color:#58736e;background:#eef4f2;font-size:9px}.profile-summary dl{margin:18px 0 0;text-align:left}.profile-summary dl>div{padding:12px 3px;display:flex;justify-content:space-between;border-top:1px solid #ebf1ef}.profile-summary dt{color:#819692;font-size:10px}.profile-summary dd{margin:0;color:#34544f;font-size:10px}.profile-form-card{padding:25px}.profile-form{margin-top:23px}.profile-form :deep(.el-form-item){margin-bottom:21px}.profile-form :deep(.el-input__wrapper),.profile-form :deep(.el-textarea__inner){border-radius:10px;box-shadow:0 0 0 1px #dfe9e7 inset}.form-hint{margin-top:6px;color:#93a5a2;font-size:9px;line-height:1.5}.profile-form-actions{display:flex;justify-content:flex-end;gap:8px}.profile-notice{margin-top:17px;padding:17px 20px;display:flex;align-items:flex-start;gap:11px;border:1px solid #e1ebe9;border-radius:16px;color:#6f8581;background:#f9fbfa}.profile-notice>.el-icon{margin-top:1px;color:#118172;font-size:17px}.profile-notice b,.profile-notice p{margin:0}.profile-notice b{display:block;color:#365752;font-size:11px}.profile-notice p{margin-top:4px;font-size:9px;line-height:1.6}.mobile-nav{display:none}.rotating{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
:deep(.portal-dialog){border-radius:20px}:global(.portal-dialog .el-dialog__header){padding:23px 25px 12px}:global(.portal-dialog .el-dialog__body){padding:10px 25px 5px}:global(.portal-dialog .el-dialog__footer){padding:15px 25px 23px}:global(.portal-dialog .el-dialog__title){color:#214641;font-size:18px;font-weight:700}.dialog-intro{margin-bottom:20px;padding:14px;display:flex;align-items:center;gap:11px;border-radius:14px;background:#f3f8f7}.dialog-intro b,.dialog-intro p{display:block;margin:0}.dialog-intro b{font-size:12px}.dialog-intro p{margin-top:4px;color:#819692;font-size:9px}.doctor-option{display:flex;justify-content:space-between;gap:12px}.doctor-option span small{margin-left:6px;color:#93a5a2}.doctor-option i{color:#698985;font-style:normal;font-size:11px}.metric-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 16px}.metric-form-grid :deep(.el-input-number){width:calc(100% - 52px)}.metric-form-grid :deep(.el-form-item__content){display:flex}.metric-note{grid-column:1/-1}.metric-note :deep(.el-textarea){width:100%}.input-unit{width:48px;margin-left:4px;color:#819591;font-size:10px}.resident-portal :deep(.el-button){border-radius:10px}.resident-portal :deep(.el-button--primary){--el-button-bg-color:#0c7568;--el-button-border-color:#0c7568;--el-button-hover-bg-color:#13897a;--el-button-hover-border-color:#13897a}
.portal-load-alert{margin-bottom:18px}.latest-record-mark{width:70px;height:70px;display:grid;place-items:center;flex:none;border-radius:22px;color:#087166;background:rgba(255,255,255,.86);font-size:30px}.health-score-card small{display:block;margin-top:7px;color:#bfe2dc;font-size:9px;line-height:1.5}.indicator-state.recorded{color:#2f718e;background:#e3f2f8}.plan-progress.pending{padding:11px 13px;border-radius:11px;color:#758c88;background:#f5f9f8;font-size:9px}.trend-point{stroke:white;stroke-width:1.5}.systolic-point{fill:#e95b73}.diastolic-point{fill:#4e8fd2}
@media(max-width:1020px){.portal-header__inner{width:calc(100% - 30px)}.portal-nav button{padding:0 9px}.portal-nav button span{display:none}.resident-chip>span:last-child{display:none}.portal-main{width:calc(100% - 30px)}.health-score-card{width:250px}.indicator-grid{grid-template-columns:repeat(2,1fr)}.health-layout{grid-template-columns:1fr 250px}}
@media(max-width:760px){:global(body:has(.resident-portal)){overflow-x:hidden}.portal-header__inner{height:64px}.portal-header{position:relative}.portal-brand__mark{width:36px;height:36px}.portal-brand small{display:none}.portal-nav{display:none}.portal-actions .round-action{display:none}.resident-chip{margin-left:auto}.logout-action{width:34px;padding:0!important}.logout-action :deep(span){display:none}.portal-main{width:calc(100% - 24px);padding:16px 0 90px}.welcome-panel{min-height:0;padding:27px 22px 92px;border-radius:22px}.welcome-copy h1{font-size:26px}.welcome-copy p{font-size:12px}.welcome-actions{margin-top:20px;display:grid;grid-template-columns:1fr 1fr}.welcome-actions :deep(.el-button){margin:0;padding:10px 8px;font-size:11px}.health-score-card{position:absolute;right:16px;bottom:16px;left:16px;width:auto;padding:11px 15px}.score-ring{width:57px;height:57px;padding:5px}.score-ring strong{font-size:18px}.score-ring span{font-size:7px}.summary-strip{width:100%;margin:12px 0 24px;padding:9px;grid-template-columns:repeat(2,1fr)}.summary-strip>div{padding:10px 8px;border-right:0;border-bottom:1px solid #e9f0ee}.summary-strip>div:nth-child(odd){border-right:1px solid #e9f0ee}.summary-strip>div:nth-last-child(-n+2){border-bottom:0}.section-heading{align-items:start}.indicator-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.indicator-card{padding:15px}.indicator-card>strong{font-size:19px}.indicator-icon{width:35px;height:35px}.overview-grid,.health-layout,.plan-list,.profile-layout{grid-template-columns:1fr}.next-appointment{align-items:flex-start;flex-wrap:wrap}.next-appointment .appointment-person{min-width:calc(100% - 70px)}.next-appointment>.status-pill{margin-left:67px}.care-tip{align-items:flex-start}.care-tip :deep(.el-button){display:none}.subpage-hero{min-height:145px;padding:25px 22px}.subpage-hero h1{font-size:25px}.hero-illustration,.profile-hero-avatar{width:62px;height:62px;margin-right:0}.appointment-stats{padding:10px}.appointment-stats>div{padding:8px;justify-content:start}.appointment-row{align-items:flex-start;flex-wrap:wrap}.appointment-row>.doctor-avatar{display:none}.appointment-row>.appointment-main{min-width:calc(100% - 75px)}.appointment-row>.el-button{margin-left:auto}.monitor-guide{display:none}.trend-chart svg{height:145px}.profile-layout{display:flex;flex-direction:column}.profile-summary{order:2}.mobile-nav{position:fixed;right:9px;bottom:9px;left:9px;z-index:40;padding:7px 4px;display:grid;grid-template-columns:repeat(5,1fr);border:1px solid rgba(218,233,229,.9);border-radius:18px;background:rgba(255,255,255,.94);backdrop-filter:blur(18px);box-shadow:0 9px 30px rgba(22,70,64,.18)}.mobile-nav button{padding:5px 2px;display:grid;place-items:center;gap:3px;border:0;color:#8a9d9a;background:none;font-size:8px}.mobile-nav button .el-icon{font-size:18px}.mobile-nav button.active{color:var(--portal-green);font-weight:700}.metric-form-grid{grid-template-columns:1fr}:global(.portal-dialog){width:calc(100% - 24px)!important}.record-table{min-width:680px}}
@media(max-width:430px){.welcome-actions{grid-template-columns:1fr}.welcome-panel{padding-bottom:138px}.health-score-card{bottom:14px}.indicator-grid{grid-template-columns:1fr 1fr}.indicator-card>p{margin-top:12px}.summary-icon{width:36px;height:36px}.subpage-hero .el-button{padding:9px}.appointment-stats .summary-icon{display:none}.appointment-stats>div{justify-content:center}.chronic-card__header{align-items:flex-start}.plan-details{grid-template-columns:1fr}.plan-details>div{padding:4px;border-right:0}.profile-hero-avatar{display:none}}
</style>
