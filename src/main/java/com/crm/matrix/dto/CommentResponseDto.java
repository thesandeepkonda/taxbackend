package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDto {

    private Long id;

    private Long clientId;

    private Long employeeId;

    private String employeeName;

    private String comment;

    private String commentType;

    private LocalDateTime createdAt;
}
