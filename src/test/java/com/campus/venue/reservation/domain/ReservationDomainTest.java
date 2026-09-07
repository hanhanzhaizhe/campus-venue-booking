package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConflictCheckerTest {

    @Test
    void adjacentSlotsDoNotOverlap() {
        LocalDateTime aStart = LocalDateTime.of(2026, 9, 4, 10, 0);
        LocalDateTime aEnd = LocalDateTime.of(2026, 9, 4, 11, 0);
        LocalDateTime bStart = LocalDateTime.of(2026, 9, 4, 11, 0);
        LocalDateTime bEnd = LocalDateTime.of(2026, 9, 4, 12, 0);
        assertFalse(ConflictChecker.overlaps(aStart, aEnd, bStart, bEnd));
        assertFalse(ConflictChecker.overlaps(bStart, bEnd, aStart, aEnd));
    }

    @Test
    void crossingSlotsOverlap() {
        LocalDateTime aStart = LocalDateTime.of(2026, 9, 4, 10, 0);
        LocalDateTime aEnd = LocalDateTime.of(2026, 9, 4, 11, 0);
        LocalDateTime bStart = LocalDateTime.of(2026, 9, 4, 10, 30);
        LocalDateTime bEnd = LocalDateTime.of(2026, 9, 4, 11, 30);
        assertTrue(ConflictChecker.overlaps(aStart, aEnd, bStart, bEnd));
    }

    @Test
    void identicalSlotsOverlap() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 4, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 4, 19, 0);
        assertTrue(ConflictChecker.overlaps(start, end, start, end));
    }
}

class TimeSlotRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 4, 10, 0);

    @Test
    void halfHourAndFutureSlotOk() {
        TimeRange range = TimeSlotRules.resolve(
                NOW.toLocalDate().plusDays(1),
                java.time.LocalTime.of(10, 0),
                java.time.LocalTime.of(11, 0),
                NOW);
        assertEquals(LocalDateTime.of(2026, 9, 5, 10, 0), range.getStart());
        assertEquals(LocalDateTime.of(2026, 9, 5, 11, 0), range.getEnd());
    }

    @Test
    void notAlignedShouldFail() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                TimeSlotRules.resolve(NOW.toLocalDate().plusDays(1),
                        java.time.LocalTime.of(10, 10),
                        java.time.LocalTime.of(11, 0),
                        NOW));
        assertEquals(ErrorCode.TIME_INVALID, ex.getErrorCode());
    }

    @Test
    void beyondSevenDaysShouldFail() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                TimeSlotRules.resolve(NOW.toLocalDate().plusDays(8),
                        java.time.LocalTime.of(10, 0),
                        java.time.LocalTime.of(11, 0),
                        NOW));
        assertEquals(ErrorCode.TOO_EARLY_OR_TOO_LATE, ex.getErrorCode());
    }

    @Test
    void pastSlotTodayShouldFail() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                TimeSlotRules.resolve(NOW.toLocalDate(),
                        java.time.LocalTime.of(9, 0),
                        java.time.LocalTime.of(10, 0),
                        NOW));
        assertEquals(ErrorCode.TOO_EARLY_OR_TOO_LATE, ex.getErrorCode());
    }

    @Test
    void openHoursEndInclusive() {
        TimeRange range = new TimeRange(
                LocalDateTime.of(2026, 9, 5, 21, 30),
                LocalDateTime.of(2026, 9, 5, 22, 0));
        TimeSlotRules.assertWithinOpenHours(range, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(22, 0));
    }

    @Test
    void beyondOpenEndShouldFail() {
        TimeRange range = new TimeRange(
                LocalDateTime.of(2026, 9, 5, 21, 30),
                LocalDateTime.of(2026, 9, 5, 22, 30));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                TimeSlotRules.assertWithinOpenHours(range, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(22, 0)));
        assertEquals(ErrorCode.OUT_OF_OPEN_HOURS, ex.getErrorCode());
    }
}

class QuotaRulesTest {

    @Test
    void twoUnfinishedIsFull() {
        QuotaRules.assertWithinQuota(0);
        QuotaRules.assertWithinQuota(1);
        BusinessException ex = assertThrows(BusinessException.class, () -> QuotaRules.assertWithinQuota(2));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getErrorCode());
    }
}
