package com.campus.venue.audit;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.campus.venue.audit.entity.AdminAuditLog;
import com.campus.venue.audit.mapper.AdminAuditLogMapper;
import com.campus.venue.audit.service.AdminAuditService;
import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.entity.Reservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * §6.5：真实库 insert，验证 MyBatis-Plus 真落库（非 mock Mapper）。
 */
@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(AdminAuditService.class)
@Transactional
class AdminAuditLogMapperIT {

    @Autowired
    private AdminAuditLogMapper adminAuditLogMapper;

    @Autowired
    private AdminAuditService adminAuditService;

    @Test
    void recordCancel_persistsRowInMysql() {
        Reservation before = reservation(ReservationStatuses.CONFIRMED);
        Reservation after = reservation(ReservationStatuses.CANCELLED);

        adminAuditService.recordReservationCancel(7L, before, after);

        List<AdminAuditLog> rows = adminAuditLogMapper.selectList(null);
        assertEquals(1, rows.size());
        AdminAuditLog row = rows.get(0);
        assertNotNull(row.getId());
        assertEquals(7L, row.getOperatorId());
        assertEquals(AdminAuditActions.RESERVATION_CANCEL, row.getAction());
        assertEquals(AdminAuditResourceTypes.RESERVATION, row.getResourceType());
        assertEquals(100L, row.getReservationId());
        assertEquals(20L, row.getVenueId());
        assertTrue(row.getBeforeData().contains("CONFIRMED"));
        assertTrue(row.getAfterData().contains("CANCELLED"));
        assertNotNull(row.getCreatedAt());
    }

    @Test
    void recordReschedule_persistsBeforeAfterSlots() {
        Reservation before = reservation(ReservationStatuses.CONFIRMED);
        Reservation after = reservation(ReservationStatuses.CONFIRMED);
        after.setStartTime(LocalDateTime.of(2026, 9, 20, 14, 0));
        after.setEndTime(LocalDateTime.of(2026, 9, 20, 15, 0));

        adminAuditService.recordReservationReschedule(7L, before, after);

        AdminAuditLog row = adminAuditLogMapper.selectList(null).get(0);
        assertEquals(AdminAuditActions.RESERVATION_RESCHEDULE, row.getAction());
        assertTrue(row.getBeforeData().contains("10:00:00"));
        assertTrue(row.getAfterData().contains("14:00:00"));
    }

    private Reservation reservation(String status) {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setVenueId(20L);
        reservation.setUserId(10L);
        reservation.setStartTime(LocalDateTime.of(2026, 9, 20, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 9, 20, 11, 0));
        reservation.setStatus(status);
        reservation.setPurpose("demo");
        return reservation;
    }
}
