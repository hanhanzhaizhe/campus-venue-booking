import { http } from './http'
import type {
  CreateReservationBody,
  Reservation,
  ReservationFilter,
  RescheduleBody,
} from '@/types/api'

export function createReservation(body: CreateReservationBody) {
  return http.post<Reservation>('/api/reservations', body)
}

export function rescheduleReservation(id: number, body: RescheduleBody) {
  return http.put<Reservation>(`/api/reservations/${id}`, body)
}

export function listMyReservations(filter: ReservationFilter) {
  return http.get<Reservation[]>('/api/reservations/me', { params: { filter } })
}

export function cancelReservation(id: number) {
  return http.post<void>(`/api/reservations/${id}/cancel`)
}
