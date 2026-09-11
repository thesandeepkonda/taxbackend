package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AssignmentResponseDto {

    private Long assignmentId;

    private Long clientId;

    private String clientName;

    private Long employeeId;

    private String employeeCode;

    private String employeeName;
    private String departmentName;

    private Boolean active;

    private LocalDateTime assignedAt;

    private LocalDateTime endedAt;

    private String reason;
}