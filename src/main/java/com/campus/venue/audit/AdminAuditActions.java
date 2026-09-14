package com.campus.venue.audit;

public final class AdminAuditActions {

    public static final String RESERVATION_CANCEL = "RESERVATION_CANCEL";
    public static final String RESERVATION_RESCHEDULE = "RESERVATION_RESCHEDULE";
    /** 二期预留 */
    public static final String VENUE_CREATE = "VENUE_CREATE";
    /** 二期预留 */
    public static final String VENUE_UPDATE = "VENUE_UPDATE";

    private AdminAuditActions() {
    }
}
