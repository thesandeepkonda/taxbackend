package com.crm.matrix.dto;

import com.crm.matrix.enums.DocumentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocDocumentResponseDto {

    private Long documentId;

    private Long clientId;

    private String documentType;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private DocumentStatus status;

    // Admin review
    private String reviewComment;

    private Long reviewedById;

    private String reviewedByName;

    private LocalDateTime reviewedAt;

    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;

    private String viewUrl;

    private String downloadUrl;
}