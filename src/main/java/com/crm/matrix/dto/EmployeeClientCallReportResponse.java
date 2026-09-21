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
public class EmployeeClientCallReportResponse {

    private Long clientId;

    private String clientName;

    private String phone;

    private Long totalCalls;

    private Long answeredCalls;

    private Long notAnsweredCalls;

    private Long failedCalls;

    private Long totalTalkTimeSeconds;

    private Long totalTalkTimeMinutes;

    private LocalDateTime firstCallTime;

    private LocalDateTime lastCallTime;
}