package com.crm.matrix.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CreateAttendancePolicyRequest {

    @NotBlank(message = "Policy name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "H:mm") // Handles both "9:00" and "09:00"
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "H:mm") // Handles single-digit hours like "5:00"
    private LocalTime endTime;

    @NotNull(message = "Allowed break minutes is required")
    private Integer allowedBreakMinutes;

    @Size(max = 50, message = "Working days description cannot exceed 50 characters")
    private String workingDays;
}