package com.campus.venue.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.reservation.domain.CancelRules;
import com.campus.venue.reservation.domain.QuotaRules;
import com.campus.venue.reservation.domain.RescheduleRules;
import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.domain.TimeRange;
import com.campus.venue.reservation.domain.TimeSlotRules;
import com.campus.venue.reservation.dto.CreateReservationRequest;
import com.campus.venue.reservation.dto.RescheduleReservationRequest;
import com.campus.venue.reservation.dto.ReservationResponse;
import com.campus.venue.reservation.entity.Reservation;
import com.campus.venue.reservation.mapper.ReservationMapper;
import com.campus.venue.security.LoginUser;
import com.campus.venue.security.SecurityUtils;
import com.campus.venue.user.entity.User;
import com.campus.venue.user.mapper.UserMapper;
import com.campus.venue.venue.entity.Venue;
import com.campus.venue.venue.mapper.VenueMapper;
import com.campus.venue.venue.service.VenueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final UserMapper userMapper;
    private final VenueMapper venueMapper;
    private final VenueService venueService;
    private final ReservationMapper reservationMapper;

    public ReservationService(UserMapper userMapper,
                              VenueMapper venueMapper,
                              VenueService venueService,
                              ReservationMapper reservationMapper) {
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
        this.venueService = venueService;
        this.reservationMapper = reservationMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse create(CreateReservationRequest request) {
        LoginUser loginUser = SecurityUtils.requireCurrentUser();
        Venue venue = venueService.requireVenue(request.getVenueId());
        if (!VenueService.STATUS_ENABLED.equals(venue.getStatus())) {
            throw new BusinessException(ErrorCode.VENUE_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        TimeRange range = TimeSlotRules.resolve(request.getDate(), request.getStartTime(), request.getEndTime(), now);
        TimeSlotRules.assertWithinOpenHours(range, venue.getOpenStart(), venue.getOpenEnd());

        User lockedUser = userMapper.selectByIdForUpdate(loginUser.getUserId());
        if (lockedUser == null || !"ACTIVE".equals(lockedUser.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        Venue lockedVenue = venueMapper.selectByIdForUpdate(venue.getId());
        if (lockedVenue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        if (!VenueService.STATUS_ENABLED.equals(lockedVenue.getStatus())) {
            throw new BusinessException(ErrorCode.VENUE_DISABLED);
        }

        long unfinished = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, lockedUser.getId())
                .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                .gt(Reservation::getEndTime, now)
                .last("FOR UPDATE"));
        QuotaRules.assertWithinQuota(unfinished);

        Long duplicate = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, lockedUser.getId())
                .eq(Reservation::getVenueId, lockedVenue.getId())
                .eq(Reservation::getStartTime, range.getStart())
                .eq(Reservation::getEndTime, range.getEnd())
                .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                .last("FOR UPDATE"));
        if (duplicate > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION);
        }

        Long conflict = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getVenueId, lockedVenue.getId())
                .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                .lt(Reservation::getStartTime, range.getEnd())
                .gt(Reservation::getEndTime, range.getStart())
                .last("FOR UPDATE"));
        if (conflict > 0) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        Reservation reservation = new Reservation();
        reservation.setVenueId(lockedVenue.getId());
        reservation.setUserId(lockedUser.getId());
        reservation.setStartTime(range.getStart());
        reservation.setEndTime(range.getEnd());
        reservation.setStatus(ReservationStatuses.CONFIRMED);
        reservation.setPurpose(StringUtils.hasText(request.getPurpose()) ? request.getPurpose().trim() : null);
        reservationMapper.insert(reservation);
        return ReservationResponse.from(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse reschedule(Long id, RescheduleReservationRequest request) {
        LoginUser loginUser = SecurityUtils.requireCurrentUser();
        Reservation existing = reservationMapper.selectById(id);
        if (existing == null || !loginUser.getUserId().equals(existing.getUserId())) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        RescheduleRules.assertUserCanReschedule(existing, now);

        User lockedUser = userMapper.selectByIdForUpdate(loginUser.getUserId());
        if (lockedUser == null || !"ACTIVE".equals(lockedUser.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        Venue lockedVenue = venueMapper.selectByIdForUpdate(existing.getVenueId());
        if (lockedVenue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        if (!VenueService.STATUS_ENABLED.equals(lockedVenue.getStatus())) {
            throw new BusinessException(ErrorCode.VENUE_DISABLED);
        }

        Reservation locked = reservationMapper.selectById(id);
        if (locked == null || !loginUser.getUserId().equals(locked.getUserId())) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        now = LocalDateTime.now();
        RescheduleRules.assertUserCanReschedule(locked, now);

        TimeRange range = TimeSlotRules.resolve(request.getDate(), request.getStartTime(), request.getEndTime(), now);
        TimeSlotRules.assertWithinOpenHours(range, lockedVenue.getOpenStart(), lockedVenue.getOpenEnd());

        Long duplicate = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, lockedUser.getId())
                .eq(Reservation::getVenueId, lockedVenue.getId())
                .eq(Reservation::getStartTime, range.getStart())
                .eq(Reservation::getEndTime, range.getEnd())
                .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                .ne(Reservation::getId, locked.getId())
                .last("FOR UPDATE"));
        if (duplicate > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION);
        }

        Long conflict = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getVenueId, lockedVenue.getId())
                .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                .lt(Reservation::getStartTime, range.getEnd())
                .gt(Reservation::getEndTime, range.getStart())
                .ne(Reservation::getId, locked.getId())
                .last("FOR UPDATE"));
        if (conflict > 0) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        locked.setStartTime(range.getStart());
        locked.setEndTime(range.getEnd());
        LambdaUpdateWrapper<Reservation> update = new LambdaUpdateWrapper<Reservation>()
                .eq(Reservation::getId, locked.getId())
                .set(Reservation::getStartTime, range.getStart())
                .set(Reservation::getEndTime, range.getEnd());
        if (request.getPurpose() != null) {
            String purpose = StringUtils.hasText(request.getPurpose()) ? request.getPurpose().trim() : null;
            update.set(Reservation::getPurpose, purpose);
            locked.setPurpose(purpose);
        }
        reservationMapper.update(null, update);
        return ReservationResponse.from(locked);
    }

    public List<ReservationResponse> listMine(String filter) {
        LoginUser loginUser = SecurityUtils.requireCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, loginUser.getUserId())
                .orderByDesc(Reservation::getStartTime);
        applyFilter(wrapper, filter, now);
        return reservationMapper.selectList(wrapper).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> listAll(Long venueId, Long userId, LocalDate date) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(venueId != null, Reservation::getVenueId, venueId)
                .eq(userId != null, Reservation::getUserId, userId)
                .orderByDesc(Reservation::getStartTime);
        if (date != null) {
            wrapper.ge(Reservation::getStartTime, date.atStartOfDay())
                    .lt(Reservation::getStartTime, date.plusDays(1).atStartOfDay());
        }
        return reservationMapper.selectList(wrapper).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse cancelByUser(Long id) {
        LoginUser loginUser = SecurityUtils.requireCurrentUser();
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null || !loginUser.getUserId().equals(reservation.getUserId())) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return cancel(reservation, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse cancelByAdmin(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return cancel(reservation, true);
    }

    private ReservationResponse cancel(Reservation reservation, boolean admin) {
        venueMapper.selectByIdForUpdate(reservation.getVenueId());
        Reservation locked = reservationMapper.selectById(reservation.getId());
        if (locked == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (admin) {
            CancelRules.assertAdminCanCancel(locked, now);
        } else {
            CancelRules.assertUserCanCancel(locked, now);
        }
        locked.setStatus(ReservationStatuses.CANCELLED);
        reservationMapper.updateById(locked);
        return ReservationResponse.from(locked);
    }

    private void applyFilter(LambdaQueryWrapper<Reservation> wrapper, String filter, LocalDateTime now) {
        if (!StringUtils.hasText(filter)) {
            return;
        }
        switch (filter) {
            case "UPCOMING":
                wrapper.eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                        .gt(Reservation::getStartTime, now);
                break;
            case "ONGOING":
                wrapper.eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                        .le(Reservation::getStartTime, now)
                        .gt(Reservation::getEndTime, now);
                break;
            case "ENDED":
                wrapper.eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                        .le(Reservation::getEndTime, now);
                break;
            case "CANCELLED":
                wrapper.eq(Reservation::getStatus, ReservationStatuses.CANCELLED);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_INVALID, "filter 只能是 UPCOMING / ONGOING / ENDED / CANCELLED");
        }
    }
}
