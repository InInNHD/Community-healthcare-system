const labels: Record<string, string> = {
  DRAFT: '草稿', SUBMITTED: '已提交', PENDING: '待处理', CONFIRMED: '已确认',
  CHECKED_IN: '已签到', WAITING: '候诊中', IN_PROGRESS: '处理中', COMPLETED: '已完成',
  CANCELLED: '已取消', SIGNED: '已签署', REVIEWED: '已审方', DISPENSED: '已发药',
  ISSUED: '待支付', PAID: '已支付', REFUNDED: '已退款', CONSENTED: '居民已同意',
  ACCEPTED: '已接收', SCHEDULED: '已安排', ATTENDED: '已就诊', CLOSED: '已闭环',
  SENT: '交换成功', FAILED: '交换失败', DEAD: '待人工处理', ACTIVE: '服务中',
  CREATED: '已创建', VERIFIED: '已核验', RETURNED: '已退回', RESOLVED: '已处置',
}

export function businessStatusLabel(status?: string | null): string {
  if (!status) return '状态未知'
  return labels[status] ?? status.replaceAll('_', ' ')
}
