package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentRequestResponseDto {

    private Long requestId;

    private Long clientId;

    private String clientName;

    private String shareToken;

    private String shareUrl;

    private Boolean active;

    private Boolean submitted;

    private LocalDateTime expiresAt;

    private List<DocumentResponseDto> documents;
}