package com.crm.matrix.service;


import com.crm.matrix.dto.CallResponseDto;
import com.crm.matrix.dto.StartCallRequestDto;
import com.crm.matrix.entity.CallRecord;
import com.crm.matrix.entity.ClientAssignment;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.CallStatus;
import com.crm.matrix.repository.CallRecordRepository;
import com.crm.matrix.repository.ClientAssignmentRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CallService {

    private final CallRecordRepository callRecordRepository;
    private final ClientAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;


    // =========================================================
    // START CALL
    // =========================================================

    public CallResponseDto startCall(
            Long clientId,
            StartCallRequestDto request,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        ClientAssignment assignment =
                assignmentRepository
                        .findByClientIdAndEmployeeIdAndActiveTrue(
                                clientId,
                                employee.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client is not assigned to you"
                                )
                        );


        if (Boolean.TRUE.equals(
                assignment.getCallInProgress())) {

            throw new RuntimeException(
                    "A call is already in progress"
            );
        }


        // Mark call as active

        assignment.setCallInProgress(true);

        assignment.setLastCalledAt(
                LocalDateTime.now()
        );

        assignmentRepository.save(assignment);


        // Create call record

        CallRecord call =
                new CallRecord();

        call.setClient(
                assignment.getClient()
        );

        call.setEmployee(employee);

        call.setCallStatus(
                CallStatus.INITIATED
        );

        call.setStartedAt(
                LocalDateTime.now()
        );

        call =
                callRecordRepository.save(call);


        return CallResponseDto.builder()
                .id(call.getId())
                .clientId(call.getClient().getId())
                .employeeId(call.getEmployee().getId())
                .startTime(call.getStartedAt())
                .endTime(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .callStatus(call.getCallStatus())
                .recordingUrl(call.getRecordingUrl())
                .remarks(call.getRemarks())
                .build();
    }


    // =========================================================
    // END CALL
    // =========================================================

    public CallResponseDto endCall(
            Long callId,
            CallStatus finalStatus,
            String recordingUrl,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        CallRecord call =
                callRecordRepository.findById(callId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Call not found"
                                )
                        );


        // Security check

        if (!call.getEmployee().getId()
                .equals(employee.getId())) {

            throw new RuntimeException(
                    "You cannot update this call"
            );
        }


        LocalDateTime endedAt =
                LocalDateTime.now();

        call.setEndedAt(endedAt);

        call.setCallStatus(finalStatus);

        call.setRecordingUrl(recordingUrl);


        // Calculate duration

        if (call.getStartedAt() != null) {

            long seconds =
                    java.time.Duration.between(
                            call.getStartedAt(),
                            endedAt
                    ).getSeconds();

            call.setDurationSeconds(seconds);
        }


        call =
                callRecordRepository.save(call);


        // IMPORTANT:
        // Call is no longer in progress

        ClientAssignment assignment =
                assignmentRepository
                        .findByClientIdAndEmployeeIdAndActiveTrue(
                                call.getClient().getId(),
                                employee.getId()
                        )
                        .orElse(null);

        if (assignment != null) {

            assignment.setCallInProgress(false);

            assignment.setLastCalledAt(
                    endedAt
            );

            assignmentRepository.save(
                    assignment
            );
        }


        return CallResponseDto.builder()
                .id(call.getId())
                .clientId(call.getClient().getId())
                .employeeId(call.getEmployee().getId())
                .startTime(call.getStartedAt())
                .endTime(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .callStatus(call.getCallStatus())
                .recordingUrl(call.getRecordingUrl())
                .remarks(call.getRemarks())
                .build();
    }


    private User getLoggedInUser(
            Authentication authentication) {

        return userRepository
                .findByEmployeeCode(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }
}