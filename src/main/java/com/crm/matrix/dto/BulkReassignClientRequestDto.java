package com.crm.matrix.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkReassignClientRequestDto {

    @NotEmpty
    private List<Long> assignmentIds;

    @NotNull
    private Long newEmployeeId;

    private String reason;
}