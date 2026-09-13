package com.campus.venue.reservation.controller;

import com.campus.venue.common.api.Result;
import com.campus.venue.reservation.dto.CreateReservationRequest;
import com.campus.venue.reservation.dto.RescheduleReservationRequest;
import com.campus.venue.reservation.dto.ReservationResponse;
import com.campus.venue.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/reservations")
@Tag(name = "3. 预约（用户）")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public Result<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        return Result.ok(reservationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ReservationResponse> reschedule(@PathVariable Long id,
                                                  @Valid @RequestBody RescheduleReservationRequest request) {
        return Result.ok(reservationService.reschedule(id, request));
    }

    @GetMapping("/me")
    public Result<List<ReservationResponse>> me(@RequestParam(required = false) String filter) {
        return Result.ok(reservationService.listMine(filter));
    }

    @PostMapping("/{id}/cancel")
    public Result<ReservationResponse> cancel(@PathVariable Long id) {
        return Result.ok(reservationService.cancelByUser(id));
    }
}
