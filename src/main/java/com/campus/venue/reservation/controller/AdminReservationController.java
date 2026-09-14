package com.campus.venue.reservation.controller;

import com.campus.venue.common.api.Result;
import com.campus.venue.reservation.dto.RescheduleReservationRequest;
import com.campus.venue.reservation.dto.ReservationResponse;
import com.campus.venue.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5. 预约（管理）")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public Result<List<ReservationResponse>> list(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.listAll(venueId, userId, date));
    }

    @PutMapping("/{id}")
    public Result<ReservationResponse> reschedule(@PathVariable Long id,
                                                  @Valid @RequestBody RescheduleReservationRequest request) {
        return Result.ok(reservationService.rescheduleByAdmin(id, request));
    }

    @PostMapping("/{id}/cancel")
    public Result<ReservationResponse> cancel(@PathVariable Long id) {
        return Result.ok(reservationService.cancelByAdmin(id));
    }
}
