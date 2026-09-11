package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrepClientResponseDto {
    private Long assignmentId;
    private Long clientId;
    private String clientName;
    private ClientStatus status;
    
    private String assignedBy;
    private LocalDateTime assignedAt;
    
    private List<DocumentResponseDto> documents;
}