package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallReportResponse {

    private Long userId;

    private String employeeCode;

    private String employeeName;

    private Long totalClients;

    private Long totalCalls;

    private Long answeredCalls;

    private Long notAnsweredCalls;

    private Long failedCalls;

    private Long totalTalkTimeSeconds;

    private Long totalTalkTimeMinutes;

    private Long averageTalkTimeSeconds;

    private LocalDateTime firstCallTime;

    private LocalDateTime lastCallTime;
}