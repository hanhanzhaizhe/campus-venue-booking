package com.campus.venue.reservation.domain;

import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;

public final class QuotaRules {

    public static final int MAX_UNFINISHED = 2;

    private QuotaRules() {
    }

    public static void assertWithinQuota(long unfinishedCount) {
        if (unfinishedCount >= MAX_UNFINISHED) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
        }
    }
}
