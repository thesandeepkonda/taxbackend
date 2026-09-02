package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocClientResponseDto {

    private Long assignmentId;
    private Long clientId;

    private String name;
    private String maskedPhone;
    private String maskedEmail;

    private ClientStatus status;
    private String currentStage;

    private String remarks;
    private LocalDateTime nextFollowUpAt;

    private Boolean callInProgress;
    private LocalDateTime lastCalledAt;
}