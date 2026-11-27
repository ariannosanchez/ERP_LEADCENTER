const LOCALE = 'es-PE'
const TIME_ZONE = 'America/Lima'

// Instances are hoisted: Intl formatters are expensive to construct.
const dateFormat = new Intl.DateTimeFormat(LOCALE, {
  timeZone: TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

const dateTimeFormat = new Intl.DateTimeFormat(LOCALE, {
  timeZone: TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const currencyFormat = new Intl.NumberFormat(LOCALE, {
  style: 'currency',
  currency: 'PEN',
})

type DateInput = string | number | Date | null | undefined

function toDate(value: DateInput): Date | null {
  if (value === null || value === undefined) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

/** Formats an instant as dd/mm/yyyy in America/Lima. Returns '' for empty or invalid input. */
export function formatDate(value: DateInput): string {
  const date = toDate(value)
  return date ? dateFormat.format(date) : ''
}

/** Formats an instant as dd/mm/yyyy, HH:mm (24h) in America/Lima. Returns '' for empty or invalid input. */
export function formatDateTime(value: DateInput): string {
  const date = toDate(value)
  return date ? dateTimeFormat.format(date) : ''
}

/** Formats an amount as Peruvian soles (S/ 1,234.50). Returns '' for empty or NaN input. */
export function formatCurrency(amount: number | null | undefined): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) return ''
  return currencyFormat.format(amount)
}
