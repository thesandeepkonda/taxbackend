package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BulkEmployeeResponse {

    private boolean success;

    private int totalRows;

    private int successRows;

    private int errorRows;

    private List<BulkEmployeeError> errors;
}