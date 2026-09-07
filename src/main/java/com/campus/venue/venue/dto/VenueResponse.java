package com.campus.venue.venue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public class VenueResponse {

    private Long id;
    private String name;
    private String type;
    private String campus;
    private String building;
    private Integer capacity;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openStart;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openEnd;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
