package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class AttendancePolicyBreakResponse {

    private Long attendancePolicyBreakId;
    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean active;
}