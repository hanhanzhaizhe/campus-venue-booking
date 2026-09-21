/** Parse "HH:mm" to minutes since midnight */
export function parseHm(hm: string): number {
  const [h, m] = hm.split(':').map(Number)
  return h * 60 + m
}

export function formatHm(totalMinutes: number): string {
  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** Half-hour slot starts from openStart inclusive to openEnd exclusive */
export function halfHourSlots(openStart: string, openEnd: string): string[] {
  const start = parseHm(openStart)
  const end = parseHm(openEnd)
  const slots: string[] = []
  for (let t = start; t < end; t += 30) {
    slots.push(formatHm(t))
  }
  return slots
}

/**
 * Reservation startTime from API: "yyyy-MM-dd HH:mm:ss"
 * User may reschedule/cancel when start - 2h >= now (exactly 2h allowed).
 */
export function canUserReschedule(startTime: string, now: Date = new Date()): boolean {
  const start = parseDateTime(startTime)
  if (!start) return false
  const deadline = new Date(start.getTime() - 2 * 60 * 60 * 1000)
  return now.getTime() <= deadline.getTime()
}

/** Admin: any time before start */
export function canAdminReschedule(startTime: string, now: Date = new Date()): boolean {
  const start = parseDateTime(startTime)
  if (!start) return false
  return now.getTime() < start.getTime()
}

export function parseDateTime(value: string): Date | null {
  if (!value) return null
  // "yyyy-MM-dd HH:mm:ss" → treat as local
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function formatDateTimeDisplay(value: string): string {
  return value || '-'
}

/** Split "yyyy-MM-dd HH:mm:ss" into date + HH:mm */
export function splitReservationTime(startTime: string): { date: string; time: string } {
  const parts = startTime.trim().split(/\s+/)
  const date = parts[0] ?? ''
  const timePart = parts[1] ?? '00:00:00'
  const time = timePart.slice(0, 5)
  return { date, time }
}

export function todayStr(): string {
  return formatDate(new Date())
}
