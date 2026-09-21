package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatContactDto {
    private Long id;
    private String name;
    private String departmentName;
    private String roleName;
    private Boolean isOnline;

}