package com.campus.venue.reservation.domain;

import java.time.LocalDateTime;

public final class ConflictChecker {

    private ConflictChecker() {
    }

    /**
     * 半开区间 [start, end) 相交：相邻（end == otherStart）不算冲突。
     */
    public static boolean overlaps(LocalDateTime start, LocalDateTime end,
                                  LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
