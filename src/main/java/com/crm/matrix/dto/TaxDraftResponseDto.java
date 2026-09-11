package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaxDraftResponseDto {
    private Long draftId;
    private Integer draftVersion;
    private String fileName;
    private String prepRemarks;
    private String adminFeedback;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime uploadedAt;
    private String prepEmployeeName;
}