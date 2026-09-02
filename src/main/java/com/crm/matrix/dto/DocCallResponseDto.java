package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocCallResponseDto {

    private Long callId;
    private Long clientId;
    private String clientName;

    private Boolean answered;

    private String startTime;
    private String endTime;

    private Long durationSeconds;

    private String recordingUrl;
}