package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeCode;

    @NotBlank(message = "Password is required")
    private String password;
}