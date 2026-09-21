package com.crm.matrix.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CallHippoReminderRequest {

    @NotNull
    private Long clientId;

    @NotNull
    @Min(1)
    private Integer reminderTime;
}