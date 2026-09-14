package com.campus.venue.audit;

import com.campus.venue.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AdminAuditSnapshots {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AdminAuditSnapshots() {
    }

    public static String reservation(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(160);
        sb.append('{');
        appendString(sb, "status", reservation.getStatus(), false);
        appendString(sb, "startTime", format(reservation.getStartTime()), true);
        appendString(sb, "endTime", format(reservation.getEndTime()), true);
        appendString(sb, "purpose", reservation.getPurpose(), true);
        appendNumber(sb, "userId", reservation.getUserId(), true);
        appendNumber(sb, "venueId", reservation.getVenueId(), true);
        sb.append('}');
        return sb.toString();
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME.format(value);
    }

    private static void appendString(StringBuilder sb, String key, String value, boolean leadingComma) {
        if (leadingComma) {
            sb.append(',');
        }
        sb.append('"').append(key).append('"').append(':');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(value)).append('"');
        }
    }

    private static void appendNumber(StringBuilder sb, String key, Long value, boolean leadingComma) {
        if (leadingComma) {
            sb.append(',');
        }
        sb.append('"').append(key).append('"').append(':');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
