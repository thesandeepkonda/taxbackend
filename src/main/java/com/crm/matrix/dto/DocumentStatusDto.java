package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentStatusDto {

    private Long documentId;

    private String documentName;

    private Boolean required;

    private Boolean uploaded;

    private String originalFileName;

    private Long fileSize;
}