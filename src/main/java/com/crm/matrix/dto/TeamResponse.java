package com.crm.matrix.dto;

import com.crm.matrix.enums.Department;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamResponse {

    private Long teamId;

    private String name;

    private Department department;

    private Long teamLeadId;

    private String teamLeadName;

    private Boolean active;
}