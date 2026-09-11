package com.crm.matrix.dto;

import com.crm.matrix.enums.DocumentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {

    private Long documentId;

    private String documentType;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private DocumentStatus status;

    private LocalDateTime uploadedAt;
}