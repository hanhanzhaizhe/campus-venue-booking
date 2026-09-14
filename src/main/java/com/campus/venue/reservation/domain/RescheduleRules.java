package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.entity.Reservation;

import java.time.LocalDateTime;

public final class RescheduleRules {

    public static final int USER_RESCHEDULE_HOURS = 2;

    private RescheduleRules() {
    }

    public static void assertUserCanReschedule(Reservation reservation, LocalDateTime now) {
        if (!ReservationStatuses.CONFIRMED.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
        }
        if (!now.isBefore(reservation.getStartTime())) {
            throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
        }
        LocalDateTime deadline = reservation.getStartTime().minusHours(USER_RESCHEDULE_HOURS);
        if (now.isAfter(deadline)) {
            throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
        }
    }
}
