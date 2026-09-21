package com.crm.matrix.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallHippoWebhookRequest {

    private String activityType;

    private String from;
    private String to;

    private String fromNumber;
    private String toNumber;

    private String dialCode;

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
    private String callerName;
    private String adminEmail;

    private String callSid;

    private String note;

    private String recordingUrl;

    private String countryName;

    private String answeredDevice;

    private String ringAnswerDuration;

    private Double billedMinutes;

    private String hangupBy;

    private java.util.List<String> tags;

    private Boolean callQueue;
}