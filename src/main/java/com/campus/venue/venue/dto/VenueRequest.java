package com.campus.venue.venue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalTime;

public class VenueRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "最多 64 字")
    private String name;

    @NotBlank(message = "不能为空")
    private String type;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "最多 32 字")
    private String campus;

    @Size(max = 64, message = "最多 64 字")
    private String building;

    @Min(value = 1, message = "至少为 1")
    private Integer capacity;

    @NotNull(message = "不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openStart;

    @NotNull(message = "不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openEnd;

    private String status;

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
