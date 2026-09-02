package com.crm.matrix.dto;

import lombok.Data;

@Data
public class EndCallRequestDto {

    private String providerCallId;

    private Boolean answered;

    private String recordingUrl;
}