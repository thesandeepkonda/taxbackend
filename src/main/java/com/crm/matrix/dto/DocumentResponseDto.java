package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponseDto {

    private Long documentId;

    private String documentType;

    private String documentName;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private Boolean uploaded;

    private Boolean verified;

    private String remarks;

    private LocalDateTime uploadedAt;
}