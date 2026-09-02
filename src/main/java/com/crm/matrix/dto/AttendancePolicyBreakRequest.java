package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class AttendancePolicyBreakRequest {

    @NotBlank(message = "Break name is required")
    private String name;

    @NotNull(message = "Break start time is required")
    private LocalTime startTime;

    @NotNull(message = "Break end time is required")
    private LocalTime endTime;
}