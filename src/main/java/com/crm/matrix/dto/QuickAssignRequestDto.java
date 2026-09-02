package com.crm.matrix.dto;

import lombok.Data;

@Data
public class QuickAssignRequestDto {
    private Long teamId;       // Send this to change team
    private Long departmentId; // Send this to change department
    private Long roleId;       // Send this to change role
}