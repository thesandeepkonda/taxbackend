package com.crm.matrix.dto;

import com.crm.matrix.enums.CallStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CallResponseDto {

    private Long id;

    private Long clientId;

    private Long employeeId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationSeconds;

    private CallStatus callStatus;

    private String recordingUrl;

    private String remarks;
}