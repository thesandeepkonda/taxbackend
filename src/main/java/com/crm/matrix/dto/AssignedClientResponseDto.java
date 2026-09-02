package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignedClientResponseDto {

    private Long clientId;

    private String name;

    private String phone;


    private String email;

    private ClientStatus status;

    private LocalDateTime nextFollowUpAt;

    private Boolean callInProgress;

    private LocalDateTime lastCalledAt;
}