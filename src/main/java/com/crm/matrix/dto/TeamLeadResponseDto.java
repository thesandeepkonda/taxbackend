package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamLeadResponseDto {
    private Long employeeId;
    private String employeeCode;
    private String fullName;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Long teamId;
    private String teamName;
}