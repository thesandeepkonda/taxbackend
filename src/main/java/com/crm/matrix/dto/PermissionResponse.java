package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {

    private Long id;
    private String code;
    private Boolean active;
}