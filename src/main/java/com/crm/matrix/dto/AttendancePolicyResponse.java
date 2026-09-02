package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalTime;

@Getter
@Builder
public class AttendancePolicyResponse {
    private Long attendancePolicyId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer allowedBreakMinutes;
    private Boolean active;
}