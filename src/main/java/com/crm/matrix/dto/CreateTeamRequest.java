package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Team name must be between 2 and 100 characters"
    )
    private String name;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}