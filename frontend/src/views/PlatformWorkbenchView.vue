<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { auth } from '../auth'
import { platformApi, type SlotView } from '../api/platform'
import ModuleStatePanel from '../components/workbench/ModuleStatePanel.vue'
import { modulesForRoles, type WorkbenchModule, type WorkbenchPortal } from '../features/workbench/module-catalog'
import { businessStatusLabel } from '../features/workbench/status-label'

const props = defineProps<{ portal: WorkbenchPortal }>()
type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error'

const modules = computed(() => modulesForRoles(props.portal, auth.user?.roles ?? []))
const selectedKey = ref('')
const state = ref<LoadState>('idle')
const errorMessage = ref('')
const items = ref<unknown[]>([])
const submitting = ref(false)

const slotId = ref<number>()
const reason = ref('')
const businessId = ref<number>()
const activeEncounterAppointmentId = ref<number>()
const activeEncounterId = ref<number>()
const activeEncounterVersion = ref<number>()
const text = ref('')
const secondaryText = ref('')
const rating = ref(5)

const selectedModule = computed<WorkbenchModule | undefined>(() => modules.value.find(module => module.key === selectedKey.value))
const title = computed(() => props.portal === 'resident' ? '居民业务中心' : props.portal === 'staff' ? '医护业务中心' : '平台治理中心')
const subtitle = computed(() => props.portal === 'resident'
  ? '预约、签约、公卫与服务协同；诊断和处方仅在线下接诊后由医生完成。'
  : props.portal === 'staff'
    ? '按岗位呈现接诊、药事、收费、公卫和转诊任务，操作人员身份由登录令牌确定。'
    : '统一管理一中心多站点、家庭医生配置、外部适配器与质量指标。')

function asRecord(item: unknown): Record<string, unknown> {
  return item && typeof item === 'object' ? item as Record<string, unknown> : {}
}

function itemTitle(item: unknown, index: number) {
  const row = asRecord(item)
  return String(row.title ?? row.name ?? row.departmentName ?? row.subject ?? row.metricCode ?? row.ticketNo ?? `业务记录 ${index + 1}`)
}

function itemDetail(item: unknown) {
  const row = asRecord(item)
  return String(row.startsAt ?? row.dueAt ?? row.targetOrganization ?? row.body ?? row.lastError ?? row.metricValue ?? row.description ?? '详细信息以业务单据为准')
}

function itemStatus(item: unknown) {
  return businessStatusLabel(String(asRecord(item).status ?? ''))
}

function firstItemId() {
  const id = asRecord(items.value[0]).id
  return typeof id === 'number' ? id : Number(id)
}

async function loadSelected() {
  state.value = 'loading'
  errorMessage.value = ''
  try {
    let response: { data: unknown[] } | undefined
    switch (selectedKey.value) {
      case 'appointment': response = await platformApi.listResidentSlots(); break
      case 'billing': response = await platformApi.listResidentInvoices(); break
      case 'contract': response = await platformApi.listResidentContracts(); break
      case 'health-programs': response = await platformApi.listResidentVisits(); break
      case 'referral': response = await platformApi.listResidentReferrals(); break
      case 'consultation': response = await platformApi.listConsultations(); break
      case 'organization': response = await platformApi.listOrganizations(); break
      case 'integration': response = await platformApi.listIntegrations(); break
      case 'quality': response = await platformApi.listQualityMetrics(); break
      default: response = { data: [] }
    }
    items.value = Array.isArray(response.data) ? response.data : []
    state.value = items.value.length ? 'success' : 'empty'
  } catch (error) {
    items.value = []
    state.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '服务连接失败，请稍后重试。'
  }
}

function selectModule(key: string) {
  selectedKey.value = key
}

async function runOperation(operation: () => Promise<unknown>, success: string) {
  submitting.value = true
  try {
    await operation()
    ElMessage.success(success)
    reason.value = ''
    text.value = ''
    secondaryText.value = ''
    await loadSelected()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '业务提交失败'
  } finally {
    submitting.value = false
  }
}

async function submitPrimaryAction() {
  const id = businessId.value ?? firstItemId()
  switch (selectedKey.value) {
    case 'appointment':
      if (!slotId.value || !reason.value.trim()) return ElMessage.warning('请选择号源并填写就诊原因')
      return runOperation(() => platformApi.createResidentAppointment({ slotId: slotId.value!, reason: reason.value.trim() }), '预约已提交')
    case 'contract':
      if (!id) return ElMessage.warning('暂无可确认合同')
      return runOperation(() => platformApi.confirmContract(id, { accepted: true }), '合同已确认')
    case 'referral':
      if (!id) return ElMessage.warning('暂无待同意转诊')
      return runOperation(() => platformApi.consentReferral(id), '已完成转诊知情同意')
    case 'record-access':
      if (!text.value.trim() || !secondaryText.value.trim()) return ElMessage.warning('请填写开放范围和用途')
      return runOperation(() => platformApi.requestRecordRelease({ scopeCode: text.value.trim(), purpose: secondaryText.value.trim() }), '档案开放申请已提交')
    case 'consultation':
      if (!text.value.trim()) return ElMessage.warning('请填写咨询内容')
      return runOperation(() => platformApi.leaveConsultation({ category: secondaryText.value.trim() || '健康咨询', content: text.value.trim() }), '健康咨询留言已提交')
    case 'feedback':
      if (!businessId.value) return ElMessage.warning('请填写已完成服务编号')
      return runOperation(() => platformApi.submitFeedback({ serviceId: businessId.value!, rating: rating.value, comment: text.value.trim() }), '感谢您的评价')
    case 'checkin-queue':
      if (!businessId.value) return ElMessage.warning('请填写预约编号')
      return runOperation(() => platformApi.checkInAppointment(businessId.value!), '签到完成')
    case 'encounter':
      if (!businessId.value || !text.value.trim()) return ElMessage.warning('请填写预约编号和主诉')
      return runOperation(async () => {
        if (activeEncounterAppointmentId.value !== businessId.value || !activeEncounterId.value) {
          const started = await platformApi.startEncounter(businessId.value!)
          activeEncounterAppointmentId.value = businessId.value
          activeEncounterId.value = started.data.id
          activeEncounterVersion.value = started.data.version
        }
        const saved = await platformApi.saveEncounterDraft(activeEncounterId.value!, {
          chiefComplaint: text.value.trim(),
          diagnosisCodes: secondaryText.value.split(',').map(value => value.trim()).filter(Boolean),
        })
        activeEncounterVersion.value = saved.data.version
        return saved
      }, '接诊已启动，病历草稿已保存')
    case 'prescription-review':
      if (!businessId.value) return ElMessage.warning('请填写处方编号')
      return runOperation(() => platformApi.reviewPrescription(businessId.value!, true, text.value.trim()), '审方完成')
    case 'prescription': {
      if (!businessId.value || !text.value.trim() || !secondaryText.value.trim()) return ElMessage.warning('请填写接诊编号、诊断和药品明细')
      const [sku, quantity, ...dosageParts] = secondaryText.value.split(':')
      const skuId = Number(sku)
      const medicineQuantity = Number(quantity)
      if (!skuId || !medicineQuantity || !dosageParts.join(':').trim()) return ElMessage.warning('药品明细格式为 SKU编号:数量:用法')
      return runOperation(() => platformApi.createPrescription({ encounterId: businessId.value!, diagnosis: text.value.trim(), items: [{ skuId, quantity: medicineQuantity, dosage: dosageParts.join(':').trim() }] }), '处方草稿已开立')
    }
    case 'dispensing':
      if (!businessId.value) return ElMessage.warning('请填写处方编号')
      return runOperation(() => platformApi.dispensePrescription(businessId.value!, { batchAllocations: [] }), '发药完成')
    case 'integration':
      if (!id) return ElMessage.warning('请填写或选择交换事件编号')
      return runOperation(() => platformApi.retryIntegration(id), '已触发交换重试')
    case 'quality':
      return runOperation(() => platformApi.refreshQualityMetrics(), '质量快照已刷新')
    default:
      return ElMessage.info('该模块等待对应业务单据后即可办理')
  }
}

async function submitSignature() {
  if (!businessId.value) return ElMessage.warning(`请填写${selectedKey.value === 'encounter' ? '接诊' : '处方'}编号`)
  if (selectedKey.value === 'encounter') {
    if (!activeEncounterId.value) return ElMessage.warning('请先启动接诊并保存病历草稿')
    return runOperation(async () => {
      const signed = await platformApi.signEncounter(activeEncounterId.value!)
      activeEncounterVersion.value = signed.data.version
      return signed
    }, '病历已签署，后续更正将保留审计痕迹')
  }
  return runOperation(() => platformApi.signPrescription(businessId.value!), '处方已签署并进入审方队列')
}

function needsBusinessId(key: string) {
  return ['feedback', 'checkin-queue', 'encounter', 'prescription', 'prescription-review', 'dispensing', 'billing-counter', 'family-doctor-tasks', 'public-health-followup', 'referral-management', 'consultation-replies', 'integration'].includes(key)
}

function actionLabel(key: string) {
  const labels: Record<string, string> = {
    appointment: '提交预约', contract: '确认首份合同', referral: '同意首份转诊', 'record-access': '申请档案开放',
    consultation: '提交非诊疗留言', feedback: '提交评价', 'checkin-queue': '确认签到', encounter: '启动接诊并保存草稿',
    prescription: '进入处方单', 'prescription-review': '通过审方', dispensing: '确认发药', 'billing-counter': '进入收费单',
    'family-doctor-tasks': '进入履约任务', 'public-health-followup': '进入随访单', 'referral-management': '进入转诊单',
    'consultation-replies': '进入留言答复', integration: '重试交换事件', quality: '刷新质量快照',
  }
  return labels[key] ?? '查看业务'
}

watch(modules, value => {
  if (!value.some(module => module.key === selectedKey.value)) selectedKey.value = value[0]?.key ?? ''
}, { immediate: true })
watch(selectedKey, () => {
  activeEncounterAppointmentId.value = undefined
  activeEncounterId.value = undefined
  activeEncounterVersion.value = undefined
  if (selectedKey.value) void loadSelected()
})
watch(businessId, value => {
  if (selectedKey.value === 'encounter' && value !== activeEncounterAppointmentId.value) {
    activeEncounterId.value = undefined
    activeEncounterVersion.value = undefined
  }
})
onMounted(() => { if (selectedKey.value && state.value === 'idle') void loadSelected() })
</script>

<template>
  <div class="platform-workbench">
    <header class="workbench-hero">
      <div><span class="hero-kicker">R2–R5 COMMUNITY CARE</span><h2>{{ title }}</h2><p>{{ subtitle }}</p></div>
      <router-link :to="`/${portal}`" class="back-link">返回门户首页</router-link>
    </header>

    <div v-if="modules.length" class="workbench-layout">
      <nav class="module-grid" :aria-label="`${title}模块`">
        <button v-for="module in modules" :key="module.key" type="button" :aria-pressed="selectedKey === module.key" :class="{ active: selectedKey === module.key }" @click="selectModule(module.key)">
          <span class="module-index">{{ String(modules.indexOf(module) + 1).padStart(2, '0') }}</span>
          <strong>{{ module.label }}</strong><small>{{ module.description }}</small><em>{{ module.statusLabel }}</em>
        </button>
      </nav>

      <section v-if="selectedModule" class="module-detail surface" aria-live="polite">
        <header><div><span>{{ selectedModule.statusLabel }}</span><h3>{{ selectedModule.label }}</h3><p>{{ selectedModule.description }}</p></div><button type="button" class="refresh-button" @click="loadSelected">刷新</button></header>

        <ModuleStatePanel :state="state" :error-message="errorMessage" :empty-message="`暂无${selectedModule.label}记录`" @retry="loadSelected">
          <div class="business-list">
            <article v-for="(item, index) in items" :key="String(asRecord(item).id ?? index)">
              <div><strong>{{ itemTitle(item, index) }}</strong><p>{{ itemDetail(item) }}</p></div><span>{{ itemStatus(item) }}</span>
            </article>
          </div>
        </ModuleStatePanel>

        <form class="operation-panel" @submit.prevent="submitPrimaryAction">
          <h4>办理操作</h4>
          <p v-if="portal === 'resident'" class="safety-note">系统从当前登录居民身份确定档案范围，请求不会提交居民 ID。健康咨询不用于线上诊断或开方。</p>
          <label v-if="selectedKey === 'appointment'">可预约号源
            <select v-model.number="slotId"><option :value="undefined">请选择</option><option v-for="slot in items as SlotView[]" :key="slot.id" :value="slot.id">{{ slot.departmentName }} · {{ slot.staffName }} · 余 {{ slot.remaining }}</option></select>
          </label>
          <label v-if="needsBusinessId(selectedKey)">{{ selectedKey === 'encounter' ? '预约编号' : '业务单据编号' }}<input v-model.number="businessId" type="number" min="1" inputmode="numeric" /></label>
          <p v-if="selectedKey === 'encounter' && activeEncounterId" class="encounter-version" role="status">当前接诊 #{{ activeEncounterId }} · 版本 {{ activeEncounterVersion }}</p>
          <label v-if="selectedKey === 'appointment'">就诊原因<textarea v-model="reason" rows="2" maxlength="500" /></label>
          <label v-if="['record-access', 'consultation', 'feedback', 'encounter', 'prescription', 'prescription-review'].includes(selectedKey)">{{ selectedKey === 'record-access' ? '开放范围' : selectedKey === 'encounter' ? '主诉' : selectedKey === 'prescription' ? '临床诊断' : selectedKey === 'prescription-review' ? '审方意见' : '内容' }}<textarea v-model="text" rows="3" maxlength="1000" /></label>
          <label v-if="['record-access', 'consultation', 'encounter', 'prescription'].includes(selectedKey)">{{ selectedKey === 'record-access' ? '使用目的' : selectedKey === 'encounter' ? '诊断编码（逗号分隔）' : selectedKey === 'prescription' ? '药品明细（SKU编号:数量:用法）' : '留言主题' }}<input v-model="secondaryText" maxlength="120" /></label>
          <label v-if="selectedKey === 'feedback'">评分<select v-model.number="rating"><option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option></select></label>
          <div class="operation-actions">
            <button type="submit" class="primary-action" :disabled="submitting">{{ submitting ? '正在提交…' : actionLabel(selectedKey) }}</button>
            <button v-if="['encounter', 'prescription'].includes(selectedKey)" type="button" class="secondary-action" :disabled="submitting" @click="submitSignature">签署确认</button>
          </div>
        </form>
      </section>
    </div>
    <div v-else class="no-access surface" role="alert"><strong>当前账号没有可用模块</strong><p>请联系管理员核对岗位与门户权限。</p></div>
  </div>
</template>

<style scoped>
.platform-workbench{display:grid;gap:20px}.workbench-hero{padding:26px 28px;display:flex;align-items:end;justify-content:space-between;border:1px solid #dceae7;border-radius:20px;background:linear-gradient(120deg,#e7f5f1,#fff 72%)}.hero-kicker{color:#0b746e;font-size:10px;font-weight:800;letter-spacing:.12em}.workbench-hero h2{margin:6px 0 7px;font-size:26px}.workbench-hero p{max-width:760px;margin:0;color:#6f8784;font-size:13px;line-height:1.7}.back-link{padding:10px 14px;border:1px solid #cde0dc;border-radius:11px;color:#0b6e69;background:white;text-decoration:none;font-size:12px}.back-link:focus-visible,.module-grid button:focus-visible,.refresh-button:focus-visible,.primary-action:focus-visible,.secondary-action:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible{outline:3px solid #8fd4c8;outline-offset:2px}.workbench-layout{display:grid;grid-template-columns:310px minmax(0,1fr);gap:18px;align-items:start}.module-grid{display:grid;gap:9px}.module-grid button{position:relative;padding:16px 17px 14px;display:grid;grid-template-columns:34px 1fr;gap:3px 10px;border:1px solid #e0eae8;border-radius:15px;color:#234744;background:rgba(255,255,255,.92);text-align:left;cursor:pointer}.module-grid button:hover,.module-grid button.active{border-color:#7ab8ae;background:#eff8f5}.module-grid button.active{box-shadow:inset 4px 0 #0b746e}.module-index{grid-row:1/4;width:30px;height:30px;display:grid;place-items:center;border-radius:9px;color:#0b746e;background:#e0f1ed;font-size:10px;font-weight:800}.module-grid strong{font-size:14px}.module-grid small{color:#7c9290;font-size:10px;line-height:1.5}.module-grid em{margin-top:4px;color:#0b746e;font-size:9px;font-style:normal;font-weight:700}.module-detail{padding:22px}.module-detail>header{display:flex;align-items:start;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid #edf2f1}.module-detail>header span{padding:4px 8px;border-radius:8px;color:#0b746e;background:#e6f4f0;font-size:9px;font-weight:700}.module-detail h3{margin:9px 0 5px;font-size:20px}.module-detail header p{margin:0;color:#7f9491;font-size:12px}.refresh-button{padding:8px 12px;border:1px solid #d3e2df;border-radius:9px;color:#547470;background:white;cursor:pointer}.business-list{display:grid;gap:8px;margin:18px 0}.business-list article{padding:13px 14px;display:flex;align-items:center;justify-content:space-between;gap:16px;border:1px solid #e5eeec;border-radius:12px}.business-list strong,.business-list p{display:block}.business-list p{margin:4px 0 0;color:#819490;font-size:10px}.business-list article>span{padding:5px 9px;border-radius:9px;color:#0b746e;background:#e7f4f1;font-size:10px;white-space:nowrap}.operation-panel{margin-top:18px;padding:18px;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 15px;border-radius:15px;background:#f6faf9}.operation-panel h4,.operation-panel>p{grid-column:1/-1}.operation-panel h4{margin:0;font-size:15px}.safety-note{margin:0;padding:10px 12px;border-left:3px solid #d1a04f;color:#7d653d;background:#fff8ea;font-size:11px;line-height:1.55}.operation-panel label{display:grid;gap:6px;color:#526d69;font-size:11px}.operation-panel input,.operation-panel select,.operation-panel textarea{width:100%;padding:9px 10px;border:1px solid #cfddda;border-radius:9px;color:#244845;background:white;resize:vertical}.operation-actions{align-self:end;display:flex;gap:8px}.primary-action,.secondary-action{min-height:39px;padding:9px 15px;border-radius:10px;cursor:pointer;font-weight:700}.primary-action{border:0;color:white;background:#0b746e}.secondary-action{border:1px solid #0b746e;color:#0b746e;background:white}.primary-action:disabled,.secondary-action:disabled{opacity:.55;cursor:wait}.no-access{padding:32px;text-align:center}.no-access p{color:#7f9290}@media(max-width:950px){.workbench-layout{grid-template-columns:1fr}.module-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.workbench-hero{align-items:start;gap:18px}.operation-panel{grid-template-columns:1fr}}@media(max-width:620px){.module-grid{grid-template-columns:1fr}.workbench-hero{padding:20px;display:grid}.operation-panel{padding:14px}}
</style>
