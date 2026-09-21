package com.campus.venue.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.venue.audit.AdminAuditActions;
import com.campus.venue.audit.AdminAuditResourceTypes;
import com.campus.venue.audit.AdminAuditSnapshots;
import com.campus.venue.audit.dto.AdminAuditLogResponse;
import com.campus.venue.audit.entity.AdminAuditLog;
import com.campus.venue.audit.mapper.AdminAuditLogMapper;
import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.entity.Reservation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAuditService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AdminAuditLogMapper adminAuditLogMapper;

    public AdminAuditService(AdminAuditLogMapper adminAuditLogMapper) {
        this.adminAuditLogMapper = adminAuditLogMapper;
    }

    public void record(Long operatorId,
                       String action,
                       String resourceType,
                       Long resourceId,
                       Long reservationId,
                       Long venueId,
                       String beforeJson,
                       String afterJson,
                       String reason) {
        AdminAuditLog log = new AdminAuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setReservationId(reservationId);
        log.setVenueId(venueId);
        log.setBeforeData(truncate(beforeJson));
        log.setAfterData(truncate(afterJson));
        log.setReason(reason);
        adminAuditLogMapper.insert(log);
    }

    public void recordReservationCancel(Long operatorId, Reservation before, Reservation after) {
        record(operatorId,
                AdminAuditActions.RESERVATION_CANCEL,
                AdminAuditResourceTypes.RESERVATION,
                after.getId(),
                after.getId(),
                after.getVenueId(),
                AdminAuditSnapshots.reservation(before),
                AdminAuditSnapshots.reservation(after),
                null);
    }

    public void recordReservationReschedule(Long operatorId, Reservation before, Reservation after) {
        record(operatorId,
                AdminAuditActions.RESERVATION_RESCHEDULE,
                AdminAuditResourceTypes.RESERVATION,
                after.getId(),
                after.getId(),
                after.getVenueId(),
                AdminAuditSnapshots.reservation(before),
                AdminAuditSnapshots.reservation(after),
                null);
    }

    public List<AdminAuditLogResponse> list(Long reservationId,
                                            Long operatorId,
                                            String action,
                                            String resourceType,
                                            Long resourceId,
                                            Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "limit 必须在 1~200");
        }
        if (StringUtils.hasText(action) && !isKnownAction(action)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "非法 action");
        }
        if (StringUtils.hasText(resourceType)
                && !AdminAuditResourceTypes.RESERVATION.equals(resourceType)
                && !AdminAuditResourceTypes.VENUE.equals(resourceType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "非法 resourceType");
        }

        LambdaQueryWrapper<AdminAuditLog> wrapper = new LambdaQueryWrapper<AdminAuditLog>()
                .eq(reservationId != null, AdminAuditLog::getReservationId, reservationId)
                .eq(operatorId != null, AdminAuditLog::getOperatorId, operatorId)
                .eq(StringUtils.hasText(action), AdminAuditLog::getAction, action)
                .eq(StringUtils.hasText(resourceType), AdminAuditLog::getResourceType, resourceType)
                .eq(resourceId != null, AdminAuditLog::getResourceId, resourceId)
                .orderByDesc(AdminAuditLog::getCreatedAt)
                .orderByDesc(AdminAuditLog::getId)
                .last("LIMIT " + size);
        return adminAuditLogMapper.selectList(wrapper).stream()
                .map(AdminAuditLogResponse::from)
                .collect(Collectors.toList());
    }

    private static boolean isKnownAction(String action) {
        return AdminAuditActions.RESERVATION_CANCEL.equals(action)
                || AdminAuditActions.RESERVATION_RESCHEDULE.equals(action)
                || AdminAuditActions.VENUE_CREATE.equals(action)
                || AdminAuditActions.VENUE_UPDATE.equals(action);
    }

    private static String truncate(String json) {
        if (json == null) {
            return null;
        }
        return json.length() <= 1000 ? json : json.substring(0, 1000);
    }
}
