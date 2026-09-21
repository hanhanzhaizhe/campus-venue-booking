import { http } from './http'
import type { Occupancy, Venue } from '@/types/api'

export function listVenues(params?: { type?: string; campus?: string }) {
  return http.get<Venue[]>('/api/venues', { params })
}

export function getVenue(id: number) {
  return http.get<Venue>(`/api/venues/${id}`)
}

export function getOccupancy(id: number, date: string) {
  return http.get<Occupancy>(`/api/venues/${id}/occupancy`, { params: { date } })
}
