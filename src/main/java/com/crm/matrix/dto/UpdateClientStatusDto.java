package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateClientStatusDto {

    @NotNull(message = "Status is required")
    private ClientStatus status;
}