package com.campus.venue.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class OccupancyResponse {

    private Long venueId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openStart;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openEnd;
    private List<OccupiedSlotResponse> occupied = new ArrayList<>();

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getOpenStart() {
        return openStart;
    }

    public void setOpenStart(LocalTime openStart) {
        this.openStart = openStart;
    }

    public LocalTime getOpenEnd() {
        return openEnd;
    }

    public void setOpenEnd(LocalTime openEnd) {
        this.openEnd = openEnd;
    }

    public List<OccupiedSlotResponse> getOccupied() {
        return occupied;
    }

    public void setOccupied(List<OccupiedSlotResponse> occupied) {
        this.occupied = occupied;
    }
}
