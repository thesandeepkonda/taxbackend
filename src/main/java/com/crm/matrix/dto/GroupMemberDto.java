package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDto {
    private Long userId;
    private String name;
    private String employeeCode;
    // Add other fields you might need (e.g., profilePictureUrl, role, etc.)
}