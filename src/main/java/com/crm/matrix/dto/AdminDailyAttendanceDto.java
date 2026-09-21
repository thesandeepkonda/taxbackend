package com.crm.matrix.dto;

import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.LeaveType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AdminDailyAttendanceDto {
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String departmentName;
    private String teamName;
    
    // Attendance Info
    private AttendanceStatus attendanceStatus;
    private LeaveType leaveType;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Long totalWorkMinutes;
    private LocalTime shiftStartTime;
}