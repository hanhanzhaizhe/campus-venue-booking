import { http } from './http'
import type { AdminAuditLog, Reservation, RescheduleBody } from '@/types/api'

export function listAdminReservations(params?: {
  venueId?: number
  userId?: number
  date?: string
}) {
  return http.get<Reservation[]>('/api/admin/reservations', { params })
}

export function adminReschedule(id: number, body: RescheduleBody) {
  return http.put<Reservation>(`/api/admin/reservations/${id}`, body)
}

export function adminCancel(id: number) {
  return http.post<void>(`/api/admin/reservations/${id}/cancel`)
}

export function listAuditLogs(params?: {
  reservationId?: number
  operatorId?: number
  action?: string
  resourceType?: string
  resourceId?: number
  limit?: number
}) {
  return http.get<AdminAuditLog[]>('/api/admin/audit-logs', { params })
}
