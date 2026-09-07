package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.entity.Reservation;

import java.time.LocalDateTime;

public final class CancelRules {

    public static final int USER_CANCEL_HOURS = 2;

    private CancelRules() {
    }

    public static void assertUserCanCancel(Reservation reservation, LocalDateTime now) {
        assertConfirmed(reservation);
        LocalDateTime deadline = reservation.getStartTime().minusHours(USER_CANCEL_HOURS);
        if (now.isAfter(deadline)) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED);
        }
    }

    public static void assertAdminCanCancel(Reservation reservation, LocalDateTime now) {
        assertConfirmed(reservation);
        if (!now.isBefore(reservation.getStartTime())) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED);
        }
    }

    private static void assertConfirmed(Reservation reservation) {
        if (!ReservationStatuses.CONFIRMED.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED);
        }
    }
}
