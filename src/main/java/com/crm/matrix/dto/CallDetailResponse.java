package com.crm.matrix.dto;

import com.crm.matrix.entity.CallHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallDetailResponse {

    private Long callHistoryId;

    private Long clientId;

    private String clientName;

    private String employeeCode;

    private String employeeName;

    private Long userId;

    private String agentId;

    private String callSid;

    private String fromNumber;

    private String toNumber;

    private String callType;

    private String status;

    private String duration;

    private Integer durationSeconds;

    private Double billedMinutes;

    private String callCharge;

    private String recordingUrl;

    private String hangupBy;

    private String answeredDevice;

    private String countryName;

    private LocalDateTime callTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;


    public static CallDetailResponse from(
            CallHistory call
    ) {

        CallDetailResponse response =
                new CallDetailResponse();

        response.setCallHistoryId(
                call.getId()
        );

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

            String firstName =
                    call.getUser().getFirstName();

            String lastName =
                    call.getUser().getLastName();

            String employeeName =
                    ((firstName == null ? "" : firstName)
                            + " "
                            + (lastName == null ? "" : lastName))
                            .trim();

            response.setEmployeeName(
                    employeeName
            );
        }

        response.setAgentId(
                call.getAgentId()
        );

        response.setCallSid(
                call.getCallSid()
        );

        response.setFromNumber(
                call.getFromNumber()
        );

        response.setToNumber(
                call.getToNumber()
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

        response.setBilledMinutes(
                call.getBilledMinutes()
        );

        response.setCallCharge(
                call.getCallCharge()
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