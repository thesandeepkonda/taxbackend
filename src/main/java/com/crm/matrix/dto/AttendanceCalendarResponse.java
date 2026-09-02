package com.crm.matrix.dto;

import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.LeaveType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceCalendarResponse {

    private LocalDate date;

    private AttendanceStatus status;

    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    private Long totalWorkMinutes;

    private Long totalBreakMinutes;

    private Long totalIdleMinutes;

    private boolean onLeave;

    private LeaveType leaveType;

    private String leaveDescription;

    private String workMode;
}