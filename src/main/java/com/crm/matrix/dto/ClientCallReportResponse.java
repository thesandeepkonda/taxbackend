package com.crm.matrix.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClientCallReportResponse(

        Long clientId,

        String clientName,

        long totalCalls,

        long answeredCalls,

        long failedCalls,

        long missedCalls,

        long notAnsweredCalls,

        long totalTalkTimeSeconds,

        double totalTalkTimeMinutes,

        List<CallDetail> calls

) {

    public record CallDetail(

            Long callHistoryId,

            Long userId,

            String employeeCode,

            String employeeName,

            String agentId,

            String callSid,

            String callType,

            String status,

            LocalDateTime callTime,

            LocalDateTime startTime,

            LocalDateTime endTime,

            Integer durationSeconds,

            double durationMinutes,

            String recordingUrl,

            String hangupBy,

            String answeredDevice

    ) {
    }
}