package com.crm.matrix.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class CreateAttendancePolicyRequest {
    @NotBlank(message = "Policy name is required")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Allowed break minutes is required")
    @Min(value = 0, message = "Allowed break minutes cannot be negative")
    private Integer allowedBreakMinutes;
}