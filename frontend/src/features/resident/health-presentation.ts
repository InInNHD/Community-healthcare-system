export type MeasurementState = '已记录' | '数据不完整' | '待上报'

export interface TrendPoint {
  index: number
  value: number
}

export function measurementStatus(...values: Array<number | null | undefined>): MeasurementState {
  const recordedCount = values.filter(value => typeof value === 'number' && Number.isFinite(value)).length
  if (recordedCount === values.length) return '已记录'
  if (recordedCount > 0) return '数据不完整'
  return '待上报'
}

export function splitRecordedSeries<K extends string>(
  records: Array<Partial<Record<K, number | null>>>,
  key: K,
): TrendPoint[][] {
  const segments: TrendPoint[][] = []
  let current: TrendPoint[] = []

  records.forEach((record, index) => {
    const value = record[key]
    if (typeof value === 'number' && Number.isFinite(value)) {
      current.push({ index, value })
      return
    }
    if (current.length > 0) segments.push(current)
    current = []
  })
  if (current.length > 0) segments.push(current)
  return segments
}

export function isRecordedToday(recordedAt: string | undefined, now = new Date()): boolean {
  if (!recordedAt) return false
  const recorded = new Date(recordedAt)
  if (Number.isNaN(recorded.getTime())) return false
  return recorded.getFullYear() === now.getFullYear()
    && recorded.getMonth() === now.getMonth()
    && recorded.getDate() === now.getDate()
}

export interface BloodPressureRecord {
  recordedAt?: string
  systolicPressure?: number | null
  diastolicPressure?: number | null
}

export function hasBloodPressureRecordedToday(
  records: BloodPressureRecord[],
  now = new Date(),
): boolean {
  return records.some(record => isRecordedToday(record.recordedAt, now)
    && typeof record.systolicPressure === 'number'
    && Number.isFinite(record.systolicPressure)
    && typeof record.diastolicPressure === 'number'
    && Number.isFinite(record.diastolicPressure))
}

export function mapTrendValue(value: number, allValues: number[]): number {
  const finiteValues = allValues.filter(Number.isFinite)
  const min = Math.min(...finiteValues, 55) - 8
  const max = Math.max(...finiteValues, 145) + 8
  return 105 - ((value - min) / Math.max(max - min, 1)) * 82
}
