package com.campus.venue.audit.dto;

import com.campus.venue.audit.entity.AdminAuditLog;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class AdminAuditLogResponse {

    private Long id;
    private Long operatorId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private Long reservationId;
    private Long venueId;
    private String beforeData;
    private String afterData;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static AdminAuditLogResponse from(AdminAuditLog log) {
        AdminAuditLogResponse response = new AdminAuditLogResponse();
        response.setId(log.getId());
        response.setOperatorId(log.getOperatorId());
        response.setAction(log.getAction());
        response.setResourceType(log.getResourceType());
        response.setResourceId(log.getResourceId());
        response.setReservationId(log.getReservationId());
        response.setVenueId(log.getVenueId());
        response.setBeforeData(log.getBeforeData());
        response.setAfterData(log.getAfterData());
        response.setReason(log.getReason());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
