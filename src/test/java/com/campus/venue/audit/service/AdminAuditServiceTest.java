package com.campus.venue.audit.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import com.campus.venue.audit.AdminAuditActions;
import com.campus.venue.audit.dto.AdminAuditLogResponse;
import com.campus.venue.audit.entity.AdminAuditLog;
import com.campus.venue.audit.mapper.AdminAuditLogMapper;
import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.entity.Reservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AdminAuditLog.class);
    }

    @Mock
    private AdminAuditLogMapper adminAuditLogMapper;

    @InjectMocks
    private AdminAuditService adminAuditService;

    @Test
    void recordReservationCancelInsertsCancelAction() {
        Reservation before = reservation(ReservationStatuses.CONFIRMED);
        Reservation after = reservation(ReservationStatuses.CANCELLED);
        adminAuditService.recordReservationCancel(7L, before, after);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogMapper).insert(captor.capture());
        AdminAuditLog log = captor.getValue();
        assertEquals(AdminAuditActions.RESERVATION_CANCEL, log.getAction());
        assertEquals(7L, log.getOperatorId());
        assertEquals(100L, log.getReservationId());
        assertEquals(20L, log.getVenueId());
        assertNull(log.getReason());
        assertTrue(log.getBeforeData().contains("CONFIRMED"));
        assertTrue(log.getAfterData().contains("CANCELLED"));
    }

    @Test
    void recordReservationRescheduleInsertsRescheduleAction() {
        Reservation before = reservation(ReservationStatuses.CONFIRMED);
        Reservation after = reservation(ReservationStatuses.CONFIRMED);
        after.setStartTime(LocalDateTime.of(2026, 9, 20, 14, 0));
        after.setEndTime(LocalDateTime.of(2026, 9, 20, 15, 0));
        adminAuditService.recordReservationReschedule(7L, before, after);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogMapper).insert(captor.capture());
        assertEquals(AdminAuditActions.RESERVATION_RESCHEDULE, captor.getValue().getAction());
        assertTrue(captor.getValue().getAfterData().contains("14:00:00"));
    }

    @Test
    void listFiltersByReservationAndOperator() {
        AdminAuditLog row = new AdminAuditLog();
        row.setId(1L);
        row.setOperatorId(7L);
        row.setReservationId(100L);
        row.setAction(AdminAuditActions.RESERVATION_CANCEL);
        row.setCreatedAt(LocalDateTime.now());
        when(adminAuditLogMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));

        List<AdminAuditLogResponse> result = adminAuditService.list(100L, 7L, null, null, null, 50);
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getOperatorId());
        assertEquals(100L, result.get(0).getReservationId());
        verify(adminAuditLogMapper).selectList(any(Wrapper.class));
    }

    @Test
    void listRejectsLimitOverMax() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminAuditService.list(null, null, null, null, null, 201));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
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
