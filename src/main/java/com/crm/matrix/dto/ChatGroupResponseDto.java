package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatGroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByName;
}