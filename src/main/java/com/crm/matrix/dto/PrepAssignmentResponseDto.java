package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PrepAssignmentResponseDto {
    private Long assignmentId;
    private Long clientId;
    private String clientName;
    private Long prepEmployeeId;
    private String prepEmployeeName;
    private String status;
    private List<DocumentResponseDto> documents; // The files handed over
}