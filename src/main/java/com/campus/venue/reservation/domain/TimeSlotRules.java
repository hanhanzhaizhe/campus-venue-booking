package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class TimeSlotRules {

    public static final int MAX_ADVANCE_DAYS = 7;
    public static final int MIN_DURATION_MINUTES = 30;

    private TimeSlotRules() {
    }

    public static TimeRange resolve(LocalDate date, LocalTime startTime, LocalTime endTime, LocalDateTime now) {
        if (date == null || startTime == null || endTime == null) {
            throw new BusinessException(ErrorCode.TIME_INVALID);
        }
        if (!aligned(startTime) || !aligned(endTime)) {
            throw new BusinessException(ErrorCode.TIME_INVALID, "开始和结束必须落在整点或半点");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.TIME_INVALID, "开始时间必须早于结束时间");
        }
        if (ChronoUnit.MINUTES.between(startTime, endTime) < MIN_DURATION_MINUTES) {
            throw new BusinessException(ErrorCode.TIME_INVALID, "时长至少 30 分钟");
        }

        LocalDate today = now.toLocalDate();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new BusinessException(ErrorCode.TOO_EARLY_OR_TOO_LATE);
        }

        LocalDateTime start = date.atTime(startTime);
        LocalDateTime end = date.atTime(endTime);
        if (!start.isAfter(now)) {
            throw new BusinessException(ErrorCode.TOO_EARLY_OR_TOO_LATE);
        }
        return new TimeRange(start, end);
    }

    public static void assertWithinOpenHours(TimeRange range, LocalTime openStart, LocalTime openEnd) {
        LocalTime start = range.getStart().toLocalTime();
        LocalTime end = range.getEnd().toLocalTime();
        if (start.isBefore(openStart) || end.isAfter(openEnd)) {
            throw new BusinessException(ErrorCode.OUT_OF_OPEN_HOURS);
        }
    }

    private static boolean aligned(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0
                && (time.getMinute() == 0 || time.getMinute() == 30);
    }
}
