package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocEmployeeService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientAssignmentRepository clientAssignmentRepository;
    private final CallLogRepository callLogRepository;
    private final ClientCommentRepository commentRepository;


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(Authentication authentication) {

        String employeeCode = authentication.getName();

        return userRepository
                .findByEmployeeCode(employeeCode)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in employee not found"));
    }


    // =========================================================
    // GET MY ASSIGNED CLIENTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getMyClients(
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        List<ClientAssignment> assignments =
                clientAssignmentRepository
                        .findByEmployeeAndActiveTrueOrderByAssignedAtDesc(employee);

        return assignments.stream()
                .map(this::convertToDto)
                .toList();
    }


    // =========================================================
    // GET ONE CLIENT
    // =========================================================

    @Transactional(readOnly = true)
    public DocClientResponseDto getMyClient(
            Long assignmentId,
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment =
                clientAssignmentRepository
                        .findByIdAndEmployeeAndActiveTrue(
                                assignmentId,
                                employee
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client is not assigned to you"
                                ));

        return convertToDto(assignment);
    }


    // =========================================================
    // UPDATE CLIENT STATUS / REMARKS / FOLLOW-UP
    // =========================================================

    public DocClientResponseDto updateClient(
            Long assignmentId,
            UpdateDocClientDto request,
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment =
                clientAssignmentRepository
                        .findByIdAndEmployeeAndActiveTrue(
                                assignmentId,
                                employee
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client is not assigned to you"
                                ));

        Client client = assignment.getClient();


        // STATUS
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }


        // FOLLOW UP
        client.setNextFollowUpAt(
                request.getNextFollowUpAt()
        );


        // SAVE CLIENT
        clientRepository.save(client);


        // REMARKS
        if (request.getRemarks() != null &&
                !request.getRemarks().trim().isEmpty()) {

            ClientComment comment = new ClientComment();

            comment.setClient(client);
            //comment.setUser(employee);
            comment.setEmployee(employee);
            comment.setAssignment(assignment);
            comment.setComment(request.getRemarks());

            commentRepository.save(comment);
        }


        return convertToDto(assignment);
    }




    public DocCallResponseDto startCall(
            Long assignmentId,
            StartCallRequestDto request,
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment =
                clientAssignmentRepository
                        .findByIdAndEmployeeAndActiveTrue(
                                assignmentId,
                                employee
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client is not assigned to you"
                                ));


        if (Boolean.TRUE.equals(
                assignment.getCallInProgress())) {

            throw new RuntimeException(
                    "Call already in progress"
            );
        }


        Client client = assignment.getClient();


        CallLog call = new CallLog();

        call.setClient(client);
        call.setEmployee(employee);
        call.setAssignment(assignment);

        call.setProviderCallId(
                request.getProviderCallId()
        );

        call.setStartTime(
                LocalDateTime.now()
        );

        call.setAnswered(false);

        callLogRepository.save(call);


        assignment.setCallInProgress(true);
        assignment.setLastCalledAt(
                LocalDateTime.now()
        );

        clientAssignmentRepository.save(assignment);


        return convertCall(call);
    }


    // =========================================================
    // END CALL
    // =========================================================

    public DocCallResponseDto endCall(
            Long callId,
            EndCallRequestDto request,
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        CallLog call =
                callLogRepository
                        .findByIdAndEmployee(
                                callId,
                                employee
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Call not found"
                                ));


        if (call.getEndTime() != null) {

            throw new RuntimeException(
                    "Call already ended"
            );
        }


        LocalDateTime endTime =
                LocalDateTime.now();

        call.setEndTime(endTime);


        // ANSWERED
        call.setAnswered(
                Boolean.TRUE.equals(
                        request.getAnswered()
                )
        );


        // RECORDING
        call.setRecordingUrl(
                request.getRecordingUrl()
        );


        // DURATION
        if (call.getStartTime() != null) {

            long seconds =
                    Duration.between(
                            call.getStartTime(),
                            endTime
                    ).getSeconds();

            call.setDurationSeconds(seconds);
        }


        callLogRepository.save(call);


        ClientAssignment assignment =
                call.getAssignment();

        assignment.setCallInProgress(false);

        assignment.setLastCalledAt(
                endTime
        );

        clientAssignmentRepository.save(assignment);


        return convertCall(call);
    }


    // =========================================================
    // GET MY FOLLOW UPS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getMyFollowUps(
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        List<ClientAssignment> assignments =
                clientAssignmentRepository
                        .findByEmployeeAndActiveTrue(employee);


        LocalDateTime now =
                LocalDateTime.now();


        return assignments.stream()

                .filter(a ->
                        a.getClient()
                                .getNextFollowUpAt() != null)

                .filter(a ->
                        !a.getClient()
                                .getNextFollowUpAt()
                                .isAfter(now))

                .map(this::convertToDto)

                .toList();
    }


    // =========================================================
    // GET NOT LIFTED CLIENTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getNotLifted(
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);


        List<ClientAssignment> assignments =
                clientAssignmentRepository
                        .findByEmployeeAndActiveTrue(employee);


        return assignments.stream()

                .filter(assignment -> {

                    List<CallLog> calls =
                            callLogRepository
                                    .findByEmployeeOrderByStartTimeDesc(
                                            employee
                                    );

                    return calls.stream()
                            .filter(call ->
                                    call.getClient()
                                            .getId()
                                            .equals(
                                                    assignment
                                                            .getClient()
                                                            .getId()
                                            ))
                            .findFirst()
                            .map(call ->
                                    !Boolean.TRUE.equals(
                                            call.getAnswered()
                                    ))
                            .orElse(false);

                })

                .map(this::convertToDto)

                .toList();
    }


    // =========================================================
    // GET MY CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocCallResponseDto> getMyCalls(
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        return callLogRepository
                .findByEmployeeOrderByStartTimeDesc(employee)
                .stream()
                .map(this::convertCall)
                .toList();
    }


    // =========================================================
    // GET CLIENT CALL HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocCallResponseDto> getClientCalls(
            Long clientId,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);


        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client not found"
                                ));


        return callLogRepository
                .findByEmployeeOrderByStartTimeDesc(employee)

                .stream()

                .filter(call ->
                        call.getClient()
                                .getId()
                                .equals(client.getId()))

                .map(this::convertCall)

                .toList();
    }


    // =========================================================
    // DTO CONVERSION
    // =========================================================

    private DocClientResponseDto convertToDto(
            ClientAssignment assignment) {

        Client client =
                assignment.getClient();


        DocClientResponseDto dto =
                new DocClientResponseDto();


        dto.setAssignmentId(
                assignment.getId()
        );

        dto.setClientId(
                client.getId()
        );

        dto.setName(
                client.getName()
        );

        dto.setMaskedPhone(
                maskPhone(client.getPhone())
        );

        dto.setMaskedEmail(
                maskEmail(client.getEmail())
        );

        dto.setStatus(
                client.getStatus()
        );

        dto.setCurrentStage(
                client.getCurrentStage()
        );

        dto.setNextFollowUpAt(
                client.getNextFollowUpAt()
        );

        dto.setCallInProgress(
                assignment.getCallInProgress()
        );

        dto.setLastCalledAt(
                assignment.getLastCalledAt()
        );

        return dto;
    }


    // =========================================================
    // PHONE MASKING
    // 9876543210 -> 9876******
    // =========================================================

    private String maskPhone(String phone) {

        if (phone == null) {
            return null;
        }

        String value =
                phone.replaceAll("\\D", "");

        if (value.length() != 10) {
            return "****";
        }

        return value.substring(0, 4)
                + "******";
    }


    // =========================================================
    // EMAIL MASKING
    // =========================================================

    private String maskEmail(String email) {

        if (email == null ||
                !email.contains("@")) {

            return "****";
        }


        String[] parts =
                email.split("@", 2);


        String username =
                parts[0];

        String domain =
                parts[1];


        if (username.length() <= 4) {

            return "****@" + domain;
        }


        String visible =
                username.substring(
                        username.length() - 4
                );


        return "****" +
                visible +
                "@" +
                domain;
    }


    // =========================================================
    // CALL DTO
    // =========================================================

    private DocCallResponseDto convertCall(
            CallLog call) {

        String start =
                call.getStartTime() == null
                        ? null
                        : call.getStartTime().toString();


        String end =
                call.getEndTime() == null
                        ? null
                        : call.getEndTime().toString();


        return new DocCallResponseDto(

                call.getId(),

                call.getClient().getId(),

                call.getClient().getName(),

                call.getAnswered(),

                start,

                end,

                call.getDurationSeconds(),

                call.getRecordingUrl()
        );
    }
}