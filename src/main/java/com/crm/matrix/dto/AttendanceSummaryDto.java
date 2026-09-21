package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummaryDto {
    private long totalEmployees;
    private long present;
    private long onLeave;
    private long absent;
}