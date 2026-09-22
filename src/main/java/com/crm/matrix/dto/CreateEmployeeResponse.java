package com.crm.matrix.dto;

import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateEmployeeResponse {

    private Long id;

    private String employeeCode;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String departmentName;
    private String roleName;



    private Long teamId;

    private String teamName;



    private Department department;
    private Role role;
    private String temporaryPassword;

    private Boolean active;

    private Long attendancePolicyId;

    private String attendancePolicyName;

    private String workMode;

    private String callHippoApiToken;

    private String callHippoFromNumber;

    private String callHippoAgentId;
}