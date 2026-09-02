package com.crm.matrix.dto;

import com.crm.matrix.enums.CallStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminCallResponseDto {

    private Long callId;

    private Long clientId;

    private String clientName;

    private Long employeeId;

    private String employeeName;

    private String provider;

    private CallStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long durationSeconds;

    private String recordingUrl;
}