package com.campus.venue.reservation.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.venue.audit.service.AdminAuditService;
import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.dto.RescheduleReservationRequest;
import com.campus.venue.reservation.dto.ReservationResponse;
import com.campus.venue.reservation.entity.Reservation;
import com.campus.venue.reservation.mapper.ReservationMapper;
import com.campus.venue.security.LoginUser;
import com.campus.venue.user.entity.User;
import com.campus.venue.user.mapper.UserMapper;
import com.campus.venue.venue.entity.Venue;
import com.campus.venue.venue.mapper.VenueMapper;
import com.campus.venue.venue.service.VenueService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceRescheduleTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long VENUE_ID = 20L;
    private static final Long RESERVATION_ID = 100L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private VenueMapper venueMapper;
    @Mock
    private VenueService venueService;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Reservation.class);
    }


    private LocalDateTime originalStart;
    private LocalDateTime originalEnd;
    private LocalDate targetDate;
    private LocalTime targetStart;
    private LocalTime targetEnd;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser(USER_ID, "alice", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        originalStart = now.plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        originalEnd = originalStart.plusHours(1);

        targetDate = LocalDate.now().plusDays(1);
        targetStart = LocalTime.of(14, 0);
        targetEnd = LocalTime.of(15, 0);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rescheduleSuccess_keepsIdAndUpdatesSlot() {
        Reservation existing = confirmedOwn("社团活动");
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, request(null));

        assertEquals(RESERVATION_ID, response.getId());
        assertEquals(VENUE_ID, response.getVenueId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(ReservationStatuses.CONFIRMED, response.getStatus());
        assertEquals(targetDate.atTime(targetStart), response.getStartTime());
        assertEquals(targetDate.atTime(targetEnd), response.getEndTime());
        assertEquals("社团活动", response.getPurpose());
        verify(reservationMapper).update(isNull(), any(Wrapper.class));
        verify(adminAuditService, never()).recordReservationReschedule(any(), any(), any());
    }

    @Test
    void rescheduleConflict_returns409() {
        Reservation existing = confirmedOwn(null);
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L, 1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(reservationMapper, never()).update(any(), any());
    }

    @Test
    void rescheduleAdjacent_succeedsWhenConflictCountZero() {
        Reservation existing = confirmedOwn(null);
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, request(null));
        assertEquals(RESERVATION_ID, response.getId());
        assertEquals(targetDate.atTime(targetStart), response.getStartTime());
    }

    @Test
    void rescheduleOthersReservation_returns404() {
        Reservation existing = confirmedOwn(null);
        existing.setUserId(OTHER_USER_ID);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
        verify(userMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void rescheduleCancelled_notAllowed() {
        Reservation existing = confirmedOwn(null);
        existing.setStatus(ReservationStatuses.CANCELLED);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void rescheduleAlreadyStarted_notAllowed() {
        Reservation existing = confirmedOwn(null);
        existing.setStartTime(LocalDateTime.now().minusMinutes(5));
        existing.setEndTime(LocalDateTime.now().plusMinutes(55));
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void rescheduleOutOfOpenHours_returns400() {
        Reservation existing = confirmedOwn(null);
        stubLocks(existing);
        RescheduleReservationRequest body = request(null);
        body.setStartTime(LocalTime.of(22, 0));
        body.setEndTime(LocalTime.of(23, 0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, body));
        assertEquals(ErrorCode.OUT_OF_OPEN_HOURS, ex.getErrorCode());
        verify(reservationMapper, never()).update(any(), any());
    }

    @Test
    void rescheduleWithinTwoHourWindow_notAllowed() {
        Reservation existing = confirmedOwn(null);
        existing.setStartTime(LocalDateTime.now().plusMinutes(90));
        existing.setEndTime(existing.getStartTime().plusHours(1));
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.reschedule(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void purposeOmitted_keepsOriginal() {
        Reservation existing = confirmedOwn("保留用途");
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, request(null));
        assertEquals("保留用途", response.getPurpose());
    }

    @Test
    void purposeBlank_clearsToNull() {
        Reservation existing = confirmedOwn("旧用途");
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, request("   "));
        assertNull(response.getPurpose());
        verify(reservationMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void purposeProvided_trimsAndUpdates() {
        Reservation existing = confirmedOwn("旧用途");
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, request("  新用途  "));
        assertEquals("新用途", response.getPurpose());
    }

    @Test
    void sameSlotOnlyChangePurpose_succeeds() {
        Reservation existing = confirmedOwn("旧");
        LocalDate sameDate = originalStart.toLocalDate();
        LocalTime sameStart = originalStart.toLocalTime();
        LocalTime sameEnd = originalEnd.toLocalTime();
        stubLocks(existing);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        RescheduleReservationRequest body = new RescheduleReservationRequest();
        body.setDate(sameDate);
        body.setStartTime(sameStart);
        body.setEndTime(sameEnd);
        body.setPurpose("只改用途");

        ReservationResponse response = reservationService.reschedule(RESERVATION_ID, body);
        assertEquals(RESERVATION_ID, response.getId());
        assertEquals(originalStart, response.getStartTime());
        assertEquals(originalEnd, response.getEndTime());
        assertEquals("只改用途", response.getPurpose());
    }

    private Reservation confirmedOwn(String purpose) {
        Reservation reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setUserId(USER_ID);
        reservation.setVenueId(VENUE_ID);
        reservation.setStartTime(originalStart);
        reservation.setEndTime(originalEnd);
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setPurpose(purpose);
        return reservation;
    }

    private void stubLocks(Reservation existing) {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        User user = new User();
        user.setId(USER_ID);
        user.setStatus("ACTIVE");
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(user);

        Venue venue = new Venue();
        venue.setId(VENUE_ID);
        venue.setStatus(VenueService.STATUS_ENABLED);
        venue.setOpenStart(LocalTime.of(8, 0));
        venue.setOpenEnd(LocalTime.of(22, 0));
        when(venueMapper.selectByIdForUpdate(VENUE_ID)).thenReturn(venue);
    }

    private RescheduleReservationRequest request(String purpose) {
        RescheduleReservationRequest body = new RescheduleReservationRequest();
        body.setDate(targetDate);
        body.setStartTime(targetStart);
        body.setEndTime(targetEnd);
        body.setPurpose(purpose);
        return body;
    }
}
