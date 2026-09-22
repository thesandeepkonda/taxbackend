package com.crm.matrix.dto;

import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.EventTargetType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class CalendarEventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventTargetType targetType;
    private Long targetId; // Used when targetType is INDIVIDUAL or TEAM
    private Department targetDepartment; // Used when targetType is DEPARTMENT
    private String meetingLink;
    private String createdByName;
}