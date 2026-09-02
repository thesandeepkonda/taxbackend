package com.crm.matrix.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateDocumentRequestDto {

    private Long clientId;

    private LocalDateTime expiresAt;

    private List<String> documentTypes;
}