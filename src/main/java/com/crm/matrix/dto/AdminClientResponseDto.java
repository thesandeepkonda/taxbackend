package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminClientResponseDto {

    private Long clientId;

    private String name;

    private String email;

    private String phone;

    private ClientStatus status;

    private String currentStage;

    private LocalDateTime nextFollowUpAt;

    private Long assignedEmployeeId;

    private String assignedEmployeeName;

    private LocalDateTime assignedAt;
}