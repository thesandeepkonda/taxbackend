package com.crm.matrix.dto;

import com.crm.matrix.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResponse {

    private Long id;

    private Long employeeId;

    private String employeeCode;
    private String employeeName;

    private LocalDate attendanceDate;

    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    private AttendanceStatus status;

    private Long totalWorkMinutes;

    private Long totalBreakMinutes;

    private Long totalIdleMinutes;

    private boolean breakActive;
    private boolean policyViolation;
    private boolean isLate;
    private boolean isEarlyCheckout;
}