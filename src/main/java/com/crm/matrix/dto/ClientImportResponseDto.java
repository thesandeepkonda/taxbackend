package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClientImportResponseDto {

    private int totalRows;

    private int successful;

    private int duplicates;

    private int invalidRows;

    private String message;
}