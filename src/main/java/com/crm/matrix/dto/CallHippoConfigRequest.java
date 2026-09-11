package com.crm.matrix.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallHippoConfigRequest {

    private String apiToken;

    private String fromNumber;

    private String agentId;
}