package com.crm.matrix.dto;

import lombok.Data;

@Data
public class CreateCommentRequestDto {

    private Long clientId;

    private Long assignmentId;

    private String comment;

    private String commentType;
}