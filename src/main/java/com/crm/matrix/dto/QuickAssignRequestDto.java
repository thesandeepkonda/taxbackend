package com.crm.matrix.dto;

import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.Role;
import lombok.Data;

@Data
public class QuickAssignRequestDto {
    private Long teamId;            // Send this to change team
    private Department department;  // Send this to change department (e.g., "PREPARATION")
    private Role role;     // Send this to change role
}