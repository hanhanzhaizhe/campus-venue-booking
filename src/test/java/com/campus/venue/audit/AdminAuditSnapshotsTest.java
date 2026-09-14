package com.campus.venue.audit;

import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.entity.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAuditSnapshotsTest {

    @Test
    void reservationSnapshotContainsKeyFields() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setStartTime(LocalDateTime.of(2026, 9, 20, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 9, 20, 11, 0));
        reservation.setPurpose("社团活动");
        reservation.setUserId(2L);
        reservation.setVenueId(1L);

        String json = AdminAuditSnapshots.reservation(reservation);
        assertTrue(json.contains("\"status\":\"CONFIRMED\""));
        assertTrue(json.contains("\"startTime\":\"2026-09-20 10:00:00\""));
        assertTrue(json.contains("\"purpose\":\"社团活动\""));
        assertTrue(json.contains("\"userId\":2"));
        assertTrue(json.contains("\"venueId\":1"));
    }
}
