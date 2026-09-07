package com.campus.venue.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.venue.common.api.ErrorCode;
import com.campus.venue.common.exception.BusinessException;
import com.campus.venue.venue.cache.VenueCache;
import com.campus.venue.venue.dto.VenueRequest;
import com.campus.venue.venue.dto.VenueResponse;
import com.campus.venue.venue.entity.Venue;
import com.campus.venue.venue.mapper.VenueMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VenueService {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    private static final Set<String> TYPES = Set.of("MEETING_ROOM", "SPORTS");
    private static final Set<String> STATUSES = Set.of(STATUS_ENABLED, STATUS_DISABLED);

    private final VenueMapper venueMapper;
    private final VenueCache venueCache;

    public VenueService(VenueMapper venueMapper, VenueCache venueCache) {
        this.venueMapper = venueMapper;
        this.venueCache = venueCache;
    }

    public List<VenueResponse> listEnabled(String type, String campus) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<Venue>()
                .eq(Venue::getStatus, STATUS_ENABLED)
                .eq(StringUtils.hasText(type), Venue::getType, type)
                .eq(StringUtils.hasText(campus), Venue::getCampus, campus)
                .orderByAsc(Venue::getId);
        return venueMapper.selectList(wrapper).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<VenueResponse> listAll(String type, String campus, String status) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<Venue>()
                .eq(StringUtils.hasText(type), Venue::getType, type)
                .eq(StringUtils.hasText(campus), Venue::getCampus, campus)
                .eq(StringUtils.hasText(status), Venue::getStatus, status)
                .orderByAsc(Venue::getId);
        return venueMapper.selectList(wrapper).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public VenueResponse getById(Long id) {
        VenueResponse cached = venueCache.get(id);
        if (cached != null) {
            return cached;
        }
        VenueResponse response = toResponse(requireVenue(id));
        venueCache.put(response);
        return response;
    }

    public VenueResponse create(VenueRequest request) {
        Venue venue = new Venue();
        fill(venue, request, true);
        venueMapper.insert(venue);
        VenueResponse response = toResponse(venue);
        venueCache.put(response);
        return response;
    }

    public VenueResponse update(Long id, VenueRequest request) {
        Venue venue = requireVenue(id);
        fill(venue, request, false);
        venueMapper.updateById(venue);
        venueCache.evict(id);
        return toResponse(venue);
    }

    public Venue requireVenue(Long id) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        return venue;
    }

    private void fill(Venue venue, VenueRequest request, boolean creating) {
        if (!TYPES.contains(request.getType())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "type 只能是 MEETING_ROOM 或 SPORTS");
        }
        if (!request.getOpenStart().isBefore(request.getOpenEnd())) {
            throw new BusinessException(ErrorCode.TIME_INVALID, "开放开始时间必须早于结束时间");
        }
        String status = request.getStatus();
        if (!StringUtils.hasText(status)) {
            status = creating ? STATUS_ENABLED : venue.getStatus();
        }
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "status 只能是 ENABLED 或 DISABLED");
        }
        venue.setName(request.getName().trim());
        venue.setType(request.getType());
        venue.setCampus(request.getCampus().trim());
        venue.setBuilding(StringUtils.hasText(request.getBuilding()) ? request.getBuilding().trim() : null);
        venue.setCapacity(request.getCapacity());
        venue.setOpenStart(request.getOpenStart());
        venue.setOpenEnd(request.getOpenEnd());
        venue.setStatus(status);
    }

    private VenueResponse toResponse(Venue venue) {
        VenueResponse response = new VenueResponse();
        response.setId(venue.getId());
        response.setName(venue.getName());
        response.setType(venue.getType());
        response.setCampus(venue.getCampus());
        response.setBuilding(venue.getBuilding());
        response.setCapacity(venue.getCapacity());
        response.setOpenStart(venue.getOpenStart());
        response.setOpenEnd(venue.getOpenEnd());
        response.setStatus(venue.getStatus());
        return response;
    }
}
