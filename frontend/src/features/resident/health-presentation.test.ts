import { describe, expect, it } from 'vitest'
import {
  hasBloodPressureRecordedToday,
  mapTrendValue,
  measurementStatus,
  splitRecordedSeries,
} from './health-presentation'

describe('resident health presentation', () => {
  it('never labels measurements as clinically normal', () => {
    expect(measurementStatus(120, 80)).toBe('已记录')
    expect(measurementStatus(120, null)).toBe('数据不完整')
    expect(measurementStatus(null, undefined)).toBe('待上报')
  })

  it('breaks a trend line when a measurement is missing instead of fabricating a point', () => {
    const records = [
      { systolicPressure: 120 },
      { systolicPressure: null },
      { systolicPressure: 126 },
      { systolicPressure: 128 },
    ]

    expect(splitRecordedSeries(records, 'systolicPressure')).toEqual([
      [{ index: 0, value: 120 }],
      [{ index: 2, value: 126 }, { index: 3, value: 128 }],
    ])
  })

  it('only marks blood pressure complete when both values exist in a record from today', () => {
    const now = new Date('2026-08-22T10:00:00+08:00')

    expect(hasBloodPressureRecordedToday([
      { recordedAt: '2026-08-22T00:01:00+08:00' },
    ], now)).toBe(false)
    expect(hasBloodPressureRecordedToday([
      { recordedAt: '2026-08-22T00:01:00+08:00', systolicPressure: 120, diastolicPressure: null },
      { recordedAt: '2026-08-22T09:00:00+08:00', systolicPressure: 118, diastolicPressure: 76 },
    ], now)).toBe(true)
    expect(hasBloodPressureRecordedToday([
      { recordedAt: '2026-08-21T23:59:00+08:00', systolicPressure: 120, diastolicPressure: 80 },
    ], now)).toBe(false)
  })

  it('maps both pressure series through one shared vertical scale', () => {
    const values = [120, 80, 126, 82]

    expect(mapTrendValue(100, values)).toBe(mapTrendValue(100, values))
    expect(mapTrendValue(126, values)).toBeLessThan(mapTrendValue(80, values))
  })
})
