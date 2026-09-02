package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmployeeCallReportDto {

    private Long employeeId;

    private String employeeCode;

    private String employeeName;

    private long assignedClients;

    private long callsMade;

    private long answeredCalls;

    private long notLiftedCalls;

    private long followUps;

    private long interested;

    private long notInterested;

    private long totalTalkTimeSeconds;

    private long totalTalkTimeMinutes;

    private long averageCallSeconds;
}