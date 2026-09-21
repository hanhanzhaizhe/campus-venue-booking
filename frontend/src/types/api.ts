export interface ApiResult<T> {
  code: string
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
}

export interface CurrentUser {
  userId: number
  username: string
  role: string
}

export interface Venue {
  id: number
  name: string
  type: string
  campus: string
  building: string
  capacity: number
  openStart: string
  openEnd: string
  status: string
}

export interface OccupiedSlot {
  reservationId: number
  startTime: string
  endTime: string
}

export interface Occupancy {
  venueId: number
  date: string
  openStart: string
  openEnd: string
  occupied: OccupiedSlot[]
}

/** Backend formats as yyyy-MM-dd HH:mm:ss */
export interface Reservation {
  id: number
  venueId: number
  userId: number
  startTime: string
  endTime: string
  status: string
  purpose: string | null
}

export interface AdminAuditLog {
  id: number
  operatorId: number
  action: string
  resourceType: string
  resourceId: number
  reservationId: number | null
  venueId: number | null
  beforeData: string | null
  afterData: string | null
  reason: string | null
  createdAt: string
}

export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN'

export type ReservationFilter = 'UPCOMING' | 'ONGOING' | 'ENDED' | 'CANCELLED'

export interface CreateReservationBody {
  venueId: number
  date: string
  startTime: string
  endTime: string
  purpose?: string
}

export interface RescheduleBody {
  date: string
  startTime: string
  endTime: string
  purpose?: string
}
