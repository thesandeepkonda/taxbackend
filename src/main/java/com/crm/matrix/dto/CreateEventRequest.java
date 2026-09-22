package com.crm.matrix.dto;

import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.EventTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateEventRequest {

    @NotBlank(message = "Event title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Target type is required")
    private EventTargetType targetType;

    @Size(max = 1000, message = "Meeting link cannot exceed 1000 characters")
    private String meetingLink;

    private Long targetId; // Required if targetType is INDIVIDUAL or TEAM

    private Department targetDepartment; // Required if targetType is DEPARTMENT
}