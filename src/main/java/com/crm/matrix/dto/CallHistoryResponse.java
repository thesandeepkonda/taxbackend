package com.crm.matrix.dto;

import com.crm.matrix.entity.CallHistory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CallHistoryResponse {

    private Long id;

    private Long clientId;
    private String clientName;

    private Long userId;
    private String employeeCode;

    private String callSid;

    private String fromNumber;
    private String toNumber;

    private String agentId;

    private String callType;
    private String status;

    private String duration;
    private Integer durationSeconds;

    private String recordingUrl;

    private String hangupBy;
    private String answeredDevice;

    private Double billedMinutes;

    private String callCharge;
    private String countryName;

    private LocalDateTime callTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public static CallHistoryResponse from(CallHistory call) {

        CallHistoryResponse response =
                new CallHistoryResponse();

        response.setId(call.getId());

        if (call.getClient() != null) {
            response.setClientId(
                    call.getClient().getId()
            );

            response.setClientName(
                    call.getClient().getName()
            );
        }

        if (call.getUser() != null) {
            response.setUserId(
                    call.getUser().getId()
            );

            response.setEmployeeCode(
                    call.getUser().getEmployeeCode()
            );
        }

        response.setCallSid(call.getCallSid());

        response.setFromNumber(
                call.getFromNumber()
        );

        response.setToNumber(
                call.getToNumber()
        );

        response.setAgentId(
                call.getAgentId()
        );

        response.setCallType(
                call.getCallType()
        );

        response.setStatus(
                call.getStatus()
        );

        response.setDuration(
                call.getDuration()
        );

        response.setDurationSeconds(
                call.getDurationSeconds()
        );

        response.setRecordingUrl(
                call.getRecordingUrl()
        );

        response.setHangupBy(
                call.getHangupBy()
        );

        response.setAnsweredDevice(
                call.getAnsweredDevice()
        );

        response.setBilledMinutes(
                call.getBilledMinutes()
        );

        response.setCallCharge(
                call.getCallCharge()
        );

        response.setCountryName(
                call.getCountryName()
        );

        response.setCallTime(
                call.getCallTime()
        );

        response.setStartTime(
                call.getStartTime()
        );

        response.setEndTime(
                call.getEndTime()
        );

        return response;
    }
}