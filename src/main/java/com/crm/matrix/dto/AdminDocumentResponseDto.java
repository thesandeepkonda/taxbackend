package com.crm.matrix.dto;

import com.crm.matrix.enums.DocumentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDocumentResponseDto {

    private Long documentId;

    private Long clientId;

    private String clientName;

    private String documentType;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private DocumentStatus status;

    private String fileUrl;

    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;


    private String reviewComment;

    private Long reviewedById;

    private String reviewedByName;

    private LocalDateTime reviewedAt;


}