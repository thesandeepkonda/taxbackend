package com.crm.matrix.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkEmployeeRequest {

    @NotEmpty(message = "Employee list cannot be empty")
    @Valid
    private List<CreateEmployeeRequest> employees;
}