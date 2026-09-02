package com.crm.matrix.dto;

import com.crm.matrix.enums.LeaveRequestStatus;
import com.crm.matrix.enums.LeaveType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveRequestResponse {

    // =========================================================
    // LEAVE
    // =========================================================

    private Long leaveId;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    private String employeeCode;

    private String employeeName;


    // =========================================================
    // LEAVE DETAILS
    // =========================================================

    private LeaveType leaveType;

    private LocalDate fromDate;

    private LocalDate toDate;

    private long totalDays;

    private String description;


    // =========================================================
    // STATUS
    // =========================================================

    private LeaveRequestStatus status;


    // =========================================================
    // APPLICATION TIME
    // =========================================================

    private LocalDateTime appliedAt;


    // =========================================================
    // ADMIN PROCESSING
    // =========================================================

    private String adminRemark;

    private String processedByName;

    private LocalDateTime processedAt;
}