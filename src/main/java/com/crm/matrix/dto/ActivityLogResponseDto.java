package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ActivityLogResponseDto {
    private Long id;
    private String performedBy;
    private String actionType;
    private String description;
    private LocalDateTime timestamp;
}