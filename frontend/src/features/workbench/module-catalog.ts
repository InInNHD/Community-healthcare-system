export type WorkbenchPortal = 'admin' | 'staff' | 'resident'
export type WorkbenchRole = 'ADMIN' | 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'REGISTRAR' | 'RESIDENT'
export type ModuleCapability = 'SELF_SERVICE' | 'CLINICAL_WRITE' | 'CARE_OPERATIONS' | 'PLATFORM_ADMIN'

export interface WorkbenchModule {
  key: string
  label: string
  description: string
  capability: ModuleCapability
  roles: WorkbenchRole[]
  statusLabel: string
}

const residentModules: WorkbenchModule[] = [
  { key: 'appointment', label: '号源预约', description: '查看可预约号源、提交预约及取消预约', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '可在线办理' },
  { key: 'billing', label: '我的账单', description: '查看门诊账单、支付与退款进度', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '费用透明' },
  { key: 'contract', label: '家庭医生签约', description: '确认服务合同并查看履约任务', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '待我确认' },
  { key: 'health-programs', label: '健康管理', description: '高血压、2 型糖尿病、COPD 与老年人健康服务', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '4 类重点人群' },
  { key: 'referral', label: '转诊进度', description: '查看转诊申请、接收与回转状态', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '全程可追踪' },
  { key: 'record-access', label: '健康档案开放', description: '查看已向本人开放的病历、报告与随访记录', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '隐私保护' },
  { key: 'consultation', label: '健康咨询留言', description: '提交非诊疗健康问题，由医护人员异步答复', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '非互联网诊疗' },
  { key: 'feedback', label: '服务评价', description: '评价已完成的社区医疗服务', capability: 'SELF_SERVICE', roles: ['RESIDENT'], statusLabel: '持续改进' },
]

const staffModules: WorkbenchModule[] = [
  { key: 'checkin-queue', label: '签到与队列', description: '为居民签到、分诊并维护候诊队列', capability: 'CARE_OPERATIONS', roles: ['NURSE', 'REGISTRAR'], statusLabel: '候诊协同' },
  { key: 'encounter', label: '接诊与病历', description: '保存病历草稿、录入诊断并签署就诊记录', capability: 'CLINICAL_WRITE', roles: ['DOCTOR'], statusLabel: '需医生签署' },
  { key: 'prescription', label: '处方开立', description: '基于线下接诊记录开立并签署处方', capability: 'CLINICAL_WRITE', roles: ['DOCTOR'], statusLabel: '线下接诊后' },
  { key: 'prescription-review', label: '处方审方', description: '审核处方适应证、剂量和用药冲突', capability: 'CARE_OPERATIONS', roles: ['PHARMACIST'], statusLabel: '药师专属' },
  { key: 'dispensing', label: '发药与批次', description: '按批次拣药、复核并完成发药', capability: 'CARE_OPERATIONS', roles: ['PHARMACIST'], statusLabel: '批次可追溯' },
  { key: 'billing-counter', label: '收费与退款', description: '收费结算、退款申请和结果查询', capability: 'CARE_OPERATIONS', roles: ['REGISTRAR'], statusLabel: '柜台业务' },
  { key: 'family-doctor-tasks', label: '家庭医生任务', description: '执行签约履约、健康评估和服务任务', capability: 'CARE_OPERATIONS', roles: ['DOCTOR', 'NURSE'], statusLabel: '团队协作' },
  { key: 'public-health-followup', label: '公卫随访', description: '完成四类重点人群随访、风险评估与核验', capability: 'CARE_OPERATIONS', roles: ['DOCTOR', 'NURSE'], statusLabel: '成人慢病优先' },
  { key: 'referral-management', label: '转诊协同', description: '发起、接收、完成与回转转诊单', capability: 'CARE_OPERATIONS', roles: ['DOCTOR', 'NURSE'], statusLabel: '上下联动' },
  { key: 'consultation-replies', label: '留言答复', description: '答复健康咨询，不提供线上诊断与处方', capability: 'CARE_OPERATIONS', roles: ['DOCTOR', 'NURSE'], statusLabel: '非诊疗答复' },
]

const adminModules: WorkbenchModule[] = [
  { key: 'organization', label: '机构与站点', description: '维护中心、下属服务站和科室层级', capability: 'PLATFORM_ADMIN', roles: ['ADMIN'], statusLabel: '一中心多站点' },
  { key: 'service-packages', label: '团队与服务包', description: '配置家庭医生团队、成员、服务包与项目', capability: 'PLATFORM_ADMIN', roles: ['ADMIN'], statusLabel: '签约基础配置' },
  { key: 'integration', label: '集成工作台', description: '监控 HIS、医保、区域平台适配器、重试与对账', capability: 'PLATFORM_ADMIN', roles: ['ADMIN'], statusLabel: '模拟适配器' },
  { key: 'quality', label: '质控指标', description: '查看病历签署、随访完成、处方审核和转诊闭环指标', capability: 'PLATFORM_ADMIN', roles: ['ADMIN'], statusLabel: '可追溯质控' },
]

const catalog: Record<WorkbenchPortal, WorkbenchModule[]> = {
  admin: adminModules,
  staff: staffModules,
  resident: residentModules,
}

export function modulesForRoles(portal: WorkbenchPortal, roles: readonly string[]): WorkbenchModule[] {
  return catalog[portal].filter(module => module.roles.some(role => roles.includes(role)))
}
