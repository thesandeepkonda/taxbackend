package com.crm.matrix.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CallHippoWebhookRequest {

    private String type;

    private String from;

    private String to;

    private String callType;

    private String duration;

    private Integer durationSeconds;

    private String status;

    private String reason;

    private String time;

    private String startTime;

    private String endTime;

    private String callCharge;

    private String email;

    private String adminEmail;

    private String callSid;

    private String countryName;

    private String answeredDevice;

    private Double billedMinutes;

    private String hangupBy;

    private String recordingUrl;

    private Map<String, Object> extraParams;
}