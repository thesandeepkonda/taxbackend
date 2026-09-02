package com.crm.matrix.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReassignClientRequestDto {

    @NotNull
    private Long newEmployeeId;

    private String reason;
}