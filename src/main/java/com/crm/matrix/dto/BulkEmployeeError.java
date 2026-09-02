package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkEmployeeError {

    private int rowNumber;

    private String field;

    private String value;

    private String message;
}