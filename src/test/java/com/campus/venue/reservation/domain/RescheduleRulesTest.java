package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.entity.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RescheduleRulesTest {

    private Reservation confirmed(LocalDateTime start) {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setStartTime(start);
        reservation.setEndTime(start.plusHours(1));
        return reservation;
    }

    @Test
    void userCanRescheduleMoreThanTwoHoursBefore() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        LocalDateTime now = start.minusHours(2).minusMinutes(1);
        assertDoesNotThrow(() -> RescheduleRules.assertUserCanReschedule(confirmed(start), now));
    }

    @Test
    void userCanRescheduleExactlyTwoHoursBefore() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        LocalDateTime now = start.minusHours(2);
        assertDoesNotThrow(() -> RescheduleRules.assertUserCanReschedule(confirmed(start), now));
    }

    @Test
    void userCannotRescheduleWithinTwoHours() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                RescheduleRules.assertUserCanReschedule(confirmed(start), start.minusHours(2).plusMinutes(1)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void userCannotRescheduleAtOrAfterStart() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                RescheduleRules.assertUserCanReschedule(confirmed(start), start));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void cancelledReservationCannotReschedule() {
        Reservation reservation = confirmed(LocalDateTime.of(2026, 9, 5, 10, 0));
        reservation.setStatus(ReservationStatuses.CANCELLED);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                RescheduleRules.assertUserCanReschedule(reservation, LocalDateTime.of(2026, 9, 4, 10, 0)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void adminCanRescheduleWithinUserTwoHourWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        // 开始前 90 分钟：本人不可，管理端窗口 A 可
        assertDoesNotThrow(() -> RescheduleRules.assertAdminCanReschedule(confirmed(start), start.minusMinutes(90)));
    }

    @Test
    void adminCanRescheduleOneMinuteBeforeStart() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        assertDoesNotThrow(() -> RescheduleRules.assertAdminCanReschedule(confirmed(start), start.minusMinutes(1)));
    }

    @Test
    void adminCannotRescheduleAtOrAfterStart() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 16, 0);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                RescheduleRules.assertAdminCanReschedule(confirmed(start), start));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void adminCannotRescheduleCancelled() {
        Reservation reservation = confirmed(LocalDateTime.of(2026, 9, 5, 10, 0));
        reservation.setStatus(ReservationStatuses.CANCELLED);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                RescheduleRules.assertAdminCanReschedule(reservation, LocalDateTime.of(2026, 9, 4, 10, 0)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }
}
