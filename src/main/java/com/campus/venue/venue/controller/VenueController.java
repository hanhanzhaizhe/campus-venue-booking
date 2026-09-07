package com.campus.venue.venue.controller;

import com.campus.venue.common.api.Result;
import com.campus.venue.reservation.dto.OccupancyResponse;
import com.campus.venue.reservation.service.OccupancyService;
import com.campus.venue.venue.dto.VenueResponse;
import com.campus.venue.venue.service.VenueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@Tag(name = "2. 场地（用户）")
public class VenueController {

    private final VenueService venueService;
    private final OccupancyService occupancyService;

    public VenueController(VenueService venueService, OccupancyService occupancyService) {
        this.venueService = venueService;
        this.occupancyService = occupancyService;
    }

    @GetMapping
    public Result<List<VenueResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String campus) {
        return Result.ok(venueService.listEnabled(type, campus));
    }

    @GetMapping("/{id}")
    public Result<VenueResponse> detail(@PathVariable Long id) {
        return Result.ok(venueService.getById(id));
    }

    @GetMapping("/{id}/occupancy")
    public Result<OccupancyResponse> occupancy(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(occupancyService.getOccupancy(id, date));
    }
}
