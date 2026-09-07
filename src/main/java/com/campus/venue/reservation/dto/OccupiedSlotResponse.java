package com.campus.venue.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public class OccupiedSlotResponse {

    private Long reservationId;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public OccupiedSlotResponse() {
    }

    public OccupiedSlotResponse(Long reservationId, LocalTime startTime, LocalTime endTime) {
        this.reservationId = reservationId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
