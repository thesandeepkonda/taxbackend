package com.crm.matrix.dto;

import com.crm.matrix.enums.WorkMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    private String phone;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private Long teamId;

    private Long roleId;

    @NotNull(message = "Active status is required")
    private Boolean active;

    @NotNull(message = "Attendance policy is required")
    private Long attendancePolicyId;


    @NotNull(message = "Work mode is required")
    private WorkMode workMode;

    private String callHippoApiToken;

    private String callHippoFromNumber;

    private String callHippoAgentId;
}