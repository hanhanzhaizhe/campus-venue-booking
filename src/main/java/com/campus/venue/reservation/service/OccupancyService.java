package com.campus.venue.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.venue.reservation.domain.ReservationStatuses;
import com.campus.venue.reservation.dto.OccupancyResponse;
import com.campus.venue.reservation.dto.OccupiedSlotResponse;
import com.campus.venue.reservation.entity.Reservation;
import com.campus.venue.reservation.mapper.ReservationMapper;
import com.campus.venue.venue.entity.Venue;
import com.campus.venue.venue.service.VenueService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OccupancyService {

    private final VenueService venueService;
    private final ReservationMapper reservationMapper;

    public OccupancyService(VenueService venueService, ReservationMapper reservationMapper) {
        this.venueService = venueService;
        this.reservationMapper = reservationMapper;
    }

    /**
     * 只读：返回某日开放时间 + 当日全部 CONFIRMED 占用（含已结束）。
     * 可约格子由调用方自行计算，能否约上以提交接口为准。
     */
    public OccupancyResponse getOccupancy(Long venueId, LocalDate date) {
        Venue venue = venueService.requireVenue(venueId);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        List<OccupiedSlotResponse> occupied = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getVenueId, venueId)
                        .eq(Reservation::getStatus, ReservationStatuses.CONFIRMED)
                        .ge(Reservation::getStartTime, dayStart)
                        .lt(Reservation::getStartTime, nextDayStart)
                        .orderByAsc(Reservation::getStartTime)
        ).stream().map(item -> new OccupiedSlotResponse(
                item.getId(),
                item.getStartTime().toLocalTime(),
                item.getEndTime().toLocalTime()
        )).collect(Collectors.toList());

        OccupancyResponse response = new OccupancyResponse();
        response.setVenueId(venue.getId());
        response.setDate(date);
        response.setOpenStart(venue.getOpenStart());
        response.setOpenEnd(venue.getOpenEnd());
        response.setOccupied(occupied);
        return response;
    }
}
