package com.crm.matrix.dto;

import com.crm.matrix.entity.CallHistory;

import java.time.LocalDateTime;

public record CallHistoryResponse(

        Long id,

        Long clientId,

        String clientName,

        Long userId,

        String employeeCode,

        String employeeName,

        String agentId,

        String callSid,

        String callType,

        String status,

        String fromNumber,

        String toNumber,

        String duration,

        Integer durationSeconds,

        Double durationMinutes,

        LocalDateTime callTime,

        LocalDateTime startTime,

        LocalDateTime endTime,

        String recordingUrl,

        String hangupBy,

        String answeredDevice,

        Double billedMinutes,

        String callCharge,

        String countryName

) {

    public static CallHistoryResponse from(
            CallHistory call
    ) {

        double minutes = 0;

        if (call.getDurationSeconds() != null) {
            minutes =
                    call.getDurationSeconds() / 60.0;
        }

        return new CallHistoryResponse(

                call.getId(),

                call.getClient() != null
                        ? call.getClient().getId()
                        : null,

                call.getClient() != null
                        ? call.getClient().getName()
                        : null,

                call.getUser() != null
                        ? call.getUser().getId()
                        : null,

                call.getUser() != null
                        ? call.getUser().getEmployeeCode()
                        : null,

                getEmployeeName(call),

                call.getAgentId(),

                call.getCallSid(),

                call.getCallType(),

                call.getStatus(),

                call.getFromNumber(),

                call.getToNumber(),

                call.getDuration(),

                call.getDurationSeconds(),

                minutes,

                call.getCallTime(),

                call.getStartTime(),

                call.getEndTime(),

                call.getRecordingUrl(),

                call.getHangupBy(),

                call.getAnsweredDevice(),

                call.getBilledMinutes(),

                call.getCallCharge(),

                call.getCountryName()
        );
    }


    private static String getEmployeeName(
            CallHistory call
    ) {

        if (call.getUser() == null) {
            return null;
        }

        String first =
                call.getUser().getFirstName();

        String last =
                call.getUser().getLastName();

        if (last == null || last.isBlank()) {
            return first;
        }

        return first + " " + last;
    }
}