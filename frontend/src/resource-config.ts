export type FieldType = 'text' | 'number' | 'date' | 'datetime' | 'textarea' | 'select'
export interface FieldConfig {
  key: string
  label: string
  type?: FieldType
  required?: boolean
  width?: number
  options?: { label: string; value: string }[]
  default?: unknown
  readonly?: boolean
  immutableAfterCreate?: boolean
}
export interface ResourceConfig {
  title: string
  description: string
  endpoint: string
  searchPlaceholder: string
  fields: FieldConfig[]
  editable?: boolean
  creatable?: boolean
  statusWorkflow?: boolean
  stockAdjustable?: boolean
}

export const resources: Record<string, ResourceConfig> = {
  patients: { title: '居民档案', description: '统一管理居民基本信息与账户状态', endpoint: '/patients', searchPlaceholder: '姓名或证件号码', fields: [
    { key: 'idCard', label: '证件号码', required: true, width: 180 }, { key: 'name', label: '姓名', required: true },
    { key: 'gender', label: '性别', type: 'select', options: [{ label: '男', value: '男' }, { label: '女', value: '女' }, { label: '其他', value: '其他' }] },
    { key: 'birthDate', label: '出生日期', type: 'date' }, { key: 'phone', label: '联系电话' },
    { key: 'address', label: '家庭地址' }, { key: 'balance', label: '账户余额', type: 'number', default: 0 },
  ]},
  doctors: { title: '医生团队', description: '维护医生资质、科室与出诊安排', endpoint: '/doctors', searchPlaceholder: '医生姓名或科室', fields: [
    { key: 'employeeNo', label: '工号', required: true }, { key: 'name', label: '姓名', required: true },
    { key: 'department', label: '科室', required: true }, { key: 'title', label: '职称' }, { key: 'phone', label: '联系电话' },
    { key: 'specialty', label: '专业特长', type: 'textarea' }, { key: 'scheduleSummary', label: '出诊安排', type: 'textarea' },
  ]},
  appointments: { title: '预约诊疗', description: '管理居民预约与诊疗状态流转', endpoint: '/appointments', searchPlaceholder: '预约号或就诊原因', fields: [
    { key: 'appointmentNo', label: '预约号', width: 150 }, { key: 'patientId', label: '患者 ID', type: 'number', required: true },
    { key: 'doctorId', label: '医生 ID', type: 'number', required: true }, { key: 'scheduledAt', label: '预约时间', type: 'datetime', required: true, width: 180 },
    { key: 'status', label: '状态', type: 'select', default: 'PENDING', readonly: true, options: [
      { label: '待确认', value: 'PENDING' }, { label: '已确认', value: 'CONFIRMED' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' },
    ]}, { key: 'reason', label: '就诊原因', type: 'textarea', required: true }, { key: 'remark', label: '备注', type: 'textarea' },
  ], statusWorkflow: true },
  health: { title: '健康监测', description: '采集并追踪居民生命体征', endpoint: '/health-records', searchPlaceholder: '按患者 ID 筛选', editable: false, fields: [
    { key: 'patientId', label: '患者 ID', type: 'number', required: true }, { key: 'recordedAt', label: '采集时间', type: 'datetime', required: true, width: 180 },
    { key: 'heartRate', label: '心率', type: 'number' }, { key: 'systolicPressure', label: '收缩压', type: 'number' },
    { key: 'diastolicPressure', label: '舒张压', type: 'number' }, { key: 'bloodOxygen', label: '血氧', type: 'number' },
    { key: 'weight', label: '体重(kg)', type: 'number' }, { key: 'note', label: '记录说明', type: 'textarea' },
  ]},
  medicines: { title: '药品库存', description: '维护药品目录、价格与安全库存', endpoint: '/medicines', searchPlaceholder: '药品名称或类别', fields: [
    { key: 'name', label: '药品名称', required: true, width: 180 }, { key: 'category', label: '类别' },
    { key: 'price', label: '单价', type: 'number', required: true, default: 0 }, { key: 'stock', label: '库存', type: 'number', required: true, default: 0, immutableAfterCreate: true },
    { key: 'minimumStock', label: '安全库存', type: 'number', required: true, default: 0 }, { key: 'specification', label: '规格说明', type: 'textarea' },
  ], stockAdjustable: true },
  chronic: { title: '慢病管理', description: '建立分层分级的慢病随访档案', endpoint: '/chronic-cases', searchPlaceholder: '疾病类型或风险等级', fields: [
    { key: 'patientId', label: '患者 ID', type: 'number', required: true }, { key: 'diseaseType', label: '疾病类型', required: true },
    { key: 'riskLevel', label: '风险等级', type: 'select', required: true, options: [
      { label: '低风险', value: '低风险' }, { label: '中风险', value: '中风险' }, { label: '高风险', value: '高风险' },
    ]}, { key: 'diagnosisDate', label: '确诊日期', type: 'date', required: true }, { key: 'doctorId', label: '责任医生 ID', type: 'number' },
    { key: 'managementPlan', label: '管理计划', type: 'textarea' },
  ]},
}
