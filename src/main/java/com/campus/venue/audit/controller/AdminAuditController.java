package com.campus.venue.audit.controller;

import com.campus.venue.audit.dto.AdminAuditLogResponse;
import com.campus.venue.audit.service.AdminAuditService;
import com.campus.venue.common.api.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "6. 审计（管理）")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public Result<List<AdminAuditLogResponse>> list(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(adminAuditService.list(reservationId, operatorId, action, resourceType, resourceId, limit));
    }
}
