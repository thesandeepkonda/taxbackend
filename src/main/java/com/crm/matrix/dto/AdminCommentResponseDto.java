package com.crm.matrix.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCommentResponseDto {

    private Long id;

    private Long clientId;

    private Long employeeId;

    private String employeeName;

    private String comment;

    private String commentType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}

