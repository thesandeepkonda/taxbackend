package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamResponse {

    private Long teamId;

    private String name;

    private Long departmentId;

    private String departmentName;

    private Long teamLeadId;

    private String teamLeadName;

    private Boolean active;
}