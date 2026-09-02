package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateDocClientDto {

    private ClientStatus status;

    private String remarks;

    private LocalDateTime nextFollowUpAt;
}