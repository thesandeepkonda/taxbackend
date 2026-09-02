package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(
            max = 100,
            message = "Department name cannot exceed 100 characters"
    )
    private String name;

    @Size(
            max = 255,
            message = "Description cannot exceed 255 characters"
    )
    private String description;
}