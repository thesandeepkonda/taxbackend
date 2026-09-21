package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CallHippoEmbeddedConfig {

    private String token;
    private String email;
    private String agentId;
}