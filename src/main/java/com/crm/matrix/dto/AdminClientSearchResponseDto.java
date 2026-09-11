package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminClientSearchResponseDto {

    private Long clientId;

    private String name;

    private String email;

    private String phone;

    private ClientStatus status;

    private String currentStage;

    private LocalDateTime nextFollowUpAt;

    private Long assignedEmployeeId;

    private String assignedEmployeeName;

    private LocalDateTime assignedAt;

    private List<CommentResponseDto> comments;
}