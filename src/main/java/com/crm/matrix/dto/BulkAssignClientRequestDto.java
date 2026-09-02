package com.crm.matrix.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkAssignClientRequestDto {

    @NotEmpty
    private List<Long> clientIds;

    @NotNull
    private Long employeeId;

    private String reason;
}