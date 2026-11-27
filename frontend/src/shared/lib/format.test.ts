import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, formatDateTime } from './format'

const normalize = (value: string) => value.replace(/[\u00a0\u202f]/g, ' ')

describe('formatDate', () => {
  it('formats in America/Lima, not UTC', () => {
    // 03:00Z is 22:00 of the previous day in Lima (UTC-5).
    expect(formatDate('2024-01-01T03:00:00Z')).toBe('31/12/2023')
  })

  it('accepts Date and epoch values', () => {
    expect(formatDate(new Date('2024-06-15T12:00:00Z'))).toBe('15/06/2024')
    expect(formatDate(Date.parse('2024-06-15T12:00:00Z'))).toBe('15/06/2024')
  })

  it('returns an empty string for nullish or invalid input', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
    expect(formatDate('not a date')).toBe('')
  })
})

describe('formatDateTime', () => {
  it('formats date and 24h time in America/Lima', () => {
    expect(normalize(formatDateTime('2024-01-01T03:00:00Z'))).toBe('31/12/2023, 22:00')
  })

  it('returns an empty string for invalid input', () => {
    expect(formatDateTime('nope')).toBe('')
  })
})

describe('formatCurrency', () => {
  it('formats PEN with the es-PE locale', () => {
    expect(normalize(formatCurrency(1234.5))).toBe('S/ 1,234.50')
  })

  it('formats zero and negatives', () => {
    expect(normalize(formatCurrency(0))).toBe('S/ 0.00')
    expect(normalize(formatCurrency(-10))).toBe('-S/ 10.00')
  })

  it('returns an empty string for nullish or NaN input', () => {
    expect(formatCurrency(null)).toBe('')
    expect(formatCurrency(undefined)).toBe('')
    expect(formatCurrency(Number.NaN)).toBe('')
  })
})
