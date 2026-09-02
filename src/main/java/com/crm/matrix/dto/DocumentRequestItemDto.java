package com.crm.matrix.dto;

import lombok.Data;

@Data
public class DocumentRequestItemDto {

    private String documentName;

    private String description;

    private Boolean required;
}