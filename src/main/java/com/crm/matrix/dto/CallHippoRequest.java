package com.crm.matrix.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallHippoRequest {

    private String toNumber;

    private String fromNumber;

    private String agentId;
}