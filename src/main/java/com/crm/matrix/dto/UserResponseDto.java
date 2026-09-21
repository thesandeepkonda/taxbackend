package com.crm.matrix.dto;

import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.LeaveType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponseDto {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String departmentName;
    private String teamName;
    private AttendanceStatus attendanceStatus; // "ABSENT" or "ON_LEAVE"
    private LeaveType leaveType;        // e.g., "SICK", "CASUAL" (if ON_LEAVE)
}