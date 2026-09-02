package com.crm.matrix.dto;

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

    private Long departmentId;

    private String departmentName;

    private Long teamId;

    private String teamName;

    private Long roleId;

    private String roleName;

    private String temporaryPassword;

    private Boolean active;

    private Long attendancePolicyId;

    private String attendancePolicyName;

    private String workMode;
}