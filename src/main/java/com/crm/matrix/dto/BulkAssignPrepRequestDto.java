package com.crm.matrix.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BulkAssignPrepRequestDto {
    @NotEmpty(message = "At least one client must be selected")
    private List<Long> clientIds;

    @NotNull(message = "Preparation Employee ID is required")
    private Long prepEmployeeId;
}