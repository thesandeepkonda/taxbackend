package com.crm.matrix.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AssignPermissionsRequest {

    @NotEmpty(message = "At least one permission is required")
    private Set<Long> permissionIds;
}