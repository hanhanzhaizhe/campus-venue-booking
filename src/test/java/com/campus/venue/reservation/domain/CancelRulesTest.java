package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.entity.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelRulesTest {

    private Reservation confirmed(LocalDateTime start) {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setStartTime(start);
        reservation.setEndTime(start.plusHours(1));
        return reservation;
    }

    @Test
    void userCanCancelExactlyTwoHoursBefore() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        LocalDateTime now = start.minusHours(2);
        assertDoesNotThrow(() -> CancelRules.assertUserCanCancel(confirmed(start), now));
    }

    @Test
    void userCannotCancelWithinTwoHours() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                CancelRules.assertUserCanCancel(confirmed(start), start.minusHours(2).plusMinutes(1)));
        assertEquals(ErrorCode.CANCEL_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void adminCanCancelBeforeStart() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        assertDoesNotThrow(() -> CancelRules.assertAdminCanCancel(confirmed(start), start.minusMinutes(1)));
    }

    @Test
    void adminCannotCancelAfterStart() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                CancelRules.assertAdminCanCancel(confirmed(start), start));
        assertEquals(ErrorCode.CANCEL_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void cancelledReservationCannotCancelAgain() {
        Reservation reservation = confirmed(LocalDateTime.of(2026, 9, 5, 10, 0));
        reservation.setStatus(ReservationStatuses.CANCELLED);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                CancelRules.assertUserCanCancel(reservation, LocalDateTime.of(2026, 9, 4, 10, 0)));
        assertEquals(ErrorCode.CANCEL_NOT_ALLOWED, ex.getErrorCode());
    }
}
