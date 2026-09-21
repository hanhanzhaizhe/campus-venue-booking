package com.campus.venue.reservation.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
class ReservationServiceAdminRescheduleTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long OWNER_ID = 10L;
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
        LoginUser admin = new LoginUser(ADMIN_ID, "admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

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
    void adminRescheduleSuccess_keepsOwnerAndVenue() {
        Reservation existing = confirmedOwned("社团活动");
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.rescheduleByAdmin(RESERVATION_ID, request(null));

        assertEquals(RESERVATION_ID, response.getId());
        assertEquals(OWNER_ID, response.getUserId());
        assertEquals(VENUE_ID, response.getVenueId());
        assertEquals(ReservationStatuses.CONFIRMED, response.getStatus());
        assertEquals(targetDate.atTime(targetStart), response.getStartTime());
        assertEquals(targetDate.atTime(targetEnd), response.getEndTime());
        assertEquals("社团活动", response.getPurpose());
        verify(userMapper).selectByIdForUpdate(OWNER_ID);
    }

    @Test
    void adminRescheduleNotFound() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void adminRescheduleCancelled_notAllowed() {
        Reservation existing = confirmedOwned(null);
        existing.setStatus(ReservationStatuses.CANCELLED);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void adminRescheduleAlreadyStarted_notAllowed() {
        Reservation existing = confirmedOwned(null);
        existing.setStartTime(LocalDateTime.now().minusMinutes(5));
        existing.setEndTime(LocalDateTime.now().plusMinutes(55));
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void adminRescheduleConflict_returns409() {
        Reservation existing = confirmedOwned(null);
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L, 1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(reservationMapper, never()).update(any(), any());
    }

    @Test
    void adminRescheduleAdjacent_succeeds() {
        Reservation existing = confirmedOwned(null);
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.rescheduleByAdmin(RESERVATION_ID, request(null));
        assertEquals(RESERVATION_ID, response.getId());
    }

    @Test
    void adminRescheduleOutOfOpenHours() {
        Reservation existing = confirmedOwned(null);
        stubLocks(existing, "ACTIVE");
        RescheduleReservationRequest body = request(null);
        body.setStartTime(LocalTime.of(22, 0));
        body.setEndTime(LocalTime.of(23, 0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, body));
        assertEquals(ErrorCode.OUT_OF_OPEN_HOURS, ex.getErrorCode());
    }

    @Test
    void adminRescheduleWithinOneMinuteWindow_succeeds() {
        // 窗口 A：开始前 1 分钟仍可改（本人改约会因 2h 失败）
        Reservation existing = confirmedOwned(null);
        existing.setStartTime(LocalDateTime.now().plusMinutes(90));
        existing.setEndTime(existing.getStartTime().plusHours(1));
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.rescheduleByAdmin(RESERVATION_ID, request(null));
        assertEquals(RESERVATION_ID, response.getId());
        assertEquals(targetDate.atTime(targetStart), response.getStartTime());
    }

    @Test
    void adminPurposeBlank_clearsToNull() {
        Reservation existing = confirmedOwned("旧用途");
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.rescheduleByAdmin(RESERVATION_ID, request("  "));
        assertNull(response.getPurpose());
    }

    @Test
    void adminPurposeOmitted_keepsOriginal() {
        Reservation existing = confirmedOwned("保留");
        stubLocks(existing, "ACTIVE");
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ReservationResponse response = reservationService.rescheduleByAdmin(RESERVATION_ID, request(null));
        assertEquals("保留", response.getPurpose());
    }

    @Test
    void inactiveOwner_notAllowed() {
        Reservation existing = confirmedOwned(null);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setStatus("DISABLED");
        when(userMapper.selectByIdForUpdate(OWNER_ID)).thenReturn(owner);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reservationService.rescheduleByAdmin(RESERVATION_ID, request(null)));
        assertEquals(ErrorCode.RESCHEDULE_NOT_ALLOWED, ex.getErrorCode());
        verify(venueMapper, never()).selectByIdForUpdate(any());
    }

    private Reservation confirmedOwned(String purpose) {
        Reservation reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setUserId(OWNER_ID);
        reservation.setVenueId(VENUE_ID);
        reservation.setStartTime(originalStart);
        reservation.setEndTime(originalEnd);
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setPurpose(purpose);
        return reservation;
    }

    private void stubLocks(Reservation existing, String ownerStatus) {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(existing);

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setStatus(ownerStatus);
        when(userMapper.selectByIdForUpdate(OWNER_ID)).thenReturn(owner);

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
