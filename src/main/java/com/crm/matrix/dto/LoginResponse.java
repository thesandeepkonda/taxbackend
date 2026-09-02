package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String employeeCode;

    private String role;

    private Long departmentId;

    private String departmentName;

    private Long teamId;
    private String teamName;

    private Set<String> permissions;
}