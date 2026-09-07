package com.campus.venue.venue.controller;

import com.campus.venue.common.api.Result;
import com.campus.venue.venue.dto.VenueRequest;
import com.campus.venue.venue.dto.VenueResponse;
import com.campus.venue.venue.service.VenueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "4. 场地（管理）")
public class AdminVenueController {

    private final VenueService venueService;

    public AdminVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public Result<List<VenueResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String status) {
        return Result.ok(venueService.listAll(type, campus, status));
    }

    @PostMapping
    public Result<VenueResponse> create(@Valid @RequestBody VenueRequest request) {
        return Result.ok(venueService.create(request));
    }

    @PutMapping("/{id}")
    public Result<VenueResponse> update(@PathVariable Long id, @Valid @RequestBody VenueRequest request) {
        return Result.ok(venueService.update(id, request));
    }
}
