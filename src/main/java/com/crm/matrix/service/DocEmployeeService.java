package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.AssignmentPeriod;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
//@Transactional
public class DocEmployeeService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientAssignmentRepository clientAssignmentRepository;
    private final CallLogRepository callLogRepository;
    private final ClientCommentRepository commentRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final DocumentRequestRepository documentRequestRepository;


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(Authentication authentication) {

        String employeeCode = authentication.getName();

        return userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new RuntimeException("Logged-in employee not found"));
    }


    // =========================================================
    // GET MY ASSIGNED CLIENTS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<DocClientResponseDto> getMyClients(Pageable pageable, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        return clientAssignmentRepository.findByEmployeeAndActiveTrueOrderByAssignedAtDesc(employee, pageable).map(this::convertToDto);
    }


    // =========================================================
    // GET ONE CLIENT
    // =========================================================

    @Transactional(readOnly = true)
    public DocClientResponseDto getMyClient(Long assignmentId, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = clientAssignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));

        return convertToDto(assignment);
    }


    // =========================================================
    // UPDATE CLIENT STATUS / REMARKS / FOLLOW-UP
    // =========================================================

    @Transactional
    public DocClientResponseDto updateClient(Long assignmentId, UpdateDocClientDto request, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = clientAssignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));

        Client client = assignment.getClient();


        // STATUS
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }


        // FOLLOW UP
        client.setNextFollowUpAt(request.getNextFollowUpAt());


        // SAVE CLIENT
        clientRepository.save(client);


        // REMARKS
        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {

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



    @Transactional
    public DocCallResponseDto startCall(Long assignmentId, StartCallRequestDto request, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = clientAssignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));


        if (Boolean.TRUE.equals(assignment.getCallInProgress())) {

            throw new RuntimeException("Call already in progress");
        }


        Client client = assignment.getClient();


        CallLog call = new CallLog();

        call.setClient(client);
        call.setEmployee(employee);
        call.setAssignment(assignment);

        call.setProviderCallId(request.getProviderCallId());

        call.setStartTime(LocalDateTime.now());

        call.setAnswered(false);

        callLogRepository.save(call);


        assignment.setCallInProgress(true);
        assignment.setLastCalledAt(LocalDateTime.now());

        clientAssignmentRepository.save(assignment);


        return convertCall(call);
    }


    // =========================================================
    // END CALL
    // =========================================================

    @Transactional
    public DocCallResponseDto endCall(Long callId, EndCallRequestDto request, Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        CallLog call = callLogRepository.findByIdAndEmployee(callId, employee).orElseThrow(() -> new RuntimeException("Call not found"));


        if (call.getEndTime() != null) {

            throw new RuntimeException("Call already ended");
        }


        LocalDateTime endTime = LocalDateTime.now();

        call.setEndTime(endTime);


        // ANSWERED
        call.setAnswered(Boolean.TRUE.equals(request.getAnswered()));


        // RECORDING
        call.setRecordingUrl(request.getRecordingUrl());


        // DURATION
        if (call.getStartTime() != null) {

            long seconds = Duration.between(call.getStartTime(), endTime).getSeconds();

            call.setDurationSeconds(seconds);
        }


        callLogRepository.save(call);


        ClientAssignment assignment = call.getAssignment();

        assignment.setCallInProgress(false);

        assignment.setLastCalledAt(endTime);

        clientAssignmentRepository.save(assignment);


        return convertCall(call);
    }


    // =========================================================
    // GET MY FOLLOW UPS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getMyFollowUps(Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        List<ClientAssignment> assignments = clientAssignmentRepository.findByEmployeeAndActiveTrue(employee);


        LocalDateTime now = LocalDateTime.now();


        return assignments.stream()

                .filter(a -> a.getClient().getNextFollowUpAt() != null)

                .filter(a -> !a.getClient().getNextFollowUpAt().isAfter(now))

                .map(this::convertToDto)

                .toList();
    }


    // =========================================================
    // GET NOT LIFTED CLIENTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getNotLifted(Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        List<ClientAssignment> assignments = clientAssignmentRepository.findByEmployeeAndActiveTrue(employee);


        return assignments.stream()

                .filter(assignment -> {

                    List<CallLog> calls = callLogRepository.findByEmployeeOrderByStartTimeDesc(employee);

                    return calls.stream().filter(call -> call.getClient().getId().equals(assignment.getClient().getId())).findFirst().map(call -> !Boolean.TRUE.equals(call.getAnswered())).orElse(false);

                })

                .map(this::convertToDto)

                .toList();
    }


    // =========================================================
    // GET MY CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocCallResponseDto> getMyCalls(Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        return callLogRepository.findByEmployeeOrderByStartTimeDesc(employee).stream().map(this::convertCall).toList();
    }


    // =========================================================
    // GET CLIENT CALL HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocCallResponseDto> getClientCalls(Long clientId, Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));


        return callLogRepository.findByEmployeeOrderByStartTimeDesc(employee)

                .stream()

                .filter(call -> call.getClient().getId().equals(client.getId()))

                .map(this::convertCall)

                .toList();
    }


    // =========================================================
    // DTO CONVERSION
    // =========================================================

    private DocClientResponseDto convertToDto(ClientAssignment assignment) {

        Client client = assignment.getClient();


        DocClientResponseDto dto = new DocClientResponseDto();


        dto.setAssignmentId(assignment.getId());

        dto.setClientId(client.getId());

        dto.setName(client.getName());

        dto.setMaskedPhone(maskPhone(client.getPhone()));

        dto.setMaskedEmail(maskEmail(client.getEmail()));

        dto.setStatus(client.getStatus());

        dto.setCurrentStage(client.getCurrentStage());

        dto.setNextFollowUpAt(client.getNextFollowUpAt());

        dto.setCallInProgress(assignment.getCallInProgress());

        dto.setLastCalledAt(assignment.getLastCalledAt());

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

        String value = phone.replaceAll("\\D", "");

        if (value.length() != 10) {
            return "****";
        }

        return value.substring(0, 4) + "******";
    }


    // =========================================================
    // EMAIL MASKING
    // =========================================================

    private String maskEmail(String email) {

        if (email == null || !email.contains("@")) {

            return "****";
        }


        String[] parts = email.split("@", 2);


        String username = parts[0];

        String domain = parts[1];


        if (username.length() <= 4) {

            return "****@" + domain;
        }


        String visible = username.substring(username.length() - 4);


        return "****" + visible + "@" + domain;
    }


    private DocCallResponseDto convertCall(CallLog call) {

        String start = call.getStartTime() == null ? null : call.getStartTime().toString();


        String end = call.getEndTime() == null ? null : call.getEndTime().toString();


        return new DocCallResponseDto(

                call.getId(),

                call.getClient().getId(),

                call.getClient().getName(),

                call.getAnswered(),

                start,

                end,

                call.getDurationSeconds(),

                call.getRecordingUrl());
    }

    @Transactional
    public AdminCommentResponseDto addComment(CreateCommentRequestDto dto, Authentication authentication) {

        // Get logged-in employee
        User employee = userRepository.findByEmployeeCode(authentication.getName()).orElseThrow(() -> new RuntimeException("Logged-in employee not found"));

        // Get client
        Client client = clientRepository.findById(dto.getClientId()).orElseThrow(() -> new RuntimeException("Client not found"));

        // Create comment
        ClientComment comment = new ClientComment();

        comment.setClient(client);

        // Logged-in employee
        comment.setEmployee(employee);

        comment.setComment(dto.getComment());
        comment.setCommentType(dto.getCommentType());

        // Assignment is optional
//        if (dto.getAssignmentId() != null) {
//
//            ClientAssignment assignment = clientAssignmentRepository
//                    .findById(dto.getAssignmentId())
//                    .orElseThrow(() ->
//                            new RuntimeException("Assignment not found"));
//
//            comment.setAssignment(assignment);
//        }

        comment.setDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        // Save
        ClientComment savedComment = commentRepository.save(comment);


        AdminCommentResponseDto response = new AdminCommentResponseDto();

        response.setId(savedComment.getId());
        response.setComment(savedComment.getComment());
        response.setCommentType(savedComment.getCommentType());
        response.setCreatedAt(savedComment.getCreatedAt());
        response.setUpdatedAt(savedComment.getUpdatedAt());

        // Employee information
        response.setEmployeeId(employee.getId());

        response.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

        // Client information
        response.setClientId(client.getId());

        return response;
    }

    @Transactional
    public DocClientResponseDto updateStatus(Long assignmentId, UpdateClientStatusDto request, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = clientAssignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));

        Client client = assignment.getClient();

        client.setStatus(request.getStatus());

        client.setUpdatedAt(LocalDateTime.now());

        clientRepository.save(client);

        return convertToDto(assignment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getClientComments(Long clientId, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = clientAssignmentRepository.findByClientIdAndEmployeeAndActiveTrue(clientId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));

        return commentRepository.findByClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId).stream().map(this::mapComment).toList();
    }

    private CommentResponseDto mapComment(ClientComment comment) {

        User employee = comment.getEmployee();

        return CommentResponseDto.builder()

                .id(comment.getId())

                .clientId(comment.getClient().getId())

                .employeeId(employee.getId())

                .employeeName(employee.getFirstName() + " " + employee.getLastName())

                .comment(comment.getComment())

                .commentType(comment.getCommentType())

                .createdAt(comment.getCreatedAt())

                .build();
    }

    @Transactional
    public AdminCommentResponseDto editComment(Long commentId, UpdateCommentRequestDto request, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientComment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        if (Boolean.TRUE.equals(comment.getDeleted())) {
            throw new RuntimeException("Cannot edit deleted comment");
        }

        if (!comment.getEmployee().getId().equals(employee.getId())) {

            throw new RuntimeException("You can edit only your own comments");
        }

        comment.setComment(request.getComment().trim());

        comment.setUpdatedAt(LocalDateTime.now());

        ClientComment saved = commentRepository.save(comment);

        AdminCommentResponseDto response = new AdminCommentResponseDto();

        response.setId(saved.getId());

        response.setComment(saved.getComment());

        response.setCommentType(saved.getCommentType());

        response.setCreatedAt(saved.getCreatedAt());

        response.setUpdatedAt(saved.getUpdatedAt());

        response.setEmployeeId(employee.getId());

        response.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

        response.setClientId(saved.getClient().getId());

        return response;
    }

    @Transactional(readOnly = true)
    public List<DocClientResponseDto> getMyClientsByStatus(ClientStatus status, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        return clientAssignmentRepository.findByEmployeeAndActiveTrueAndClient_StatusOrderByAssignedAtDesc(employee, status).stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<DocClientResponseDto> searchClients(

            Long clientId, String name, AssignmentPeriod period, LocalDate fromDate, LocalDate toDate, Pageable pageable, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        // =========================================================
        // PERIOD FILTER
        // =========================================================

        if (period != null) {

            LocalDate today = LocalDate.now();

            switch (period) {

                case TODAY -> {

                    fromDateTime = today.atStartOfDay();

                    toDateTime = today.atTime(LocalTime.MAX);
                }

                case THIS_WEEK -> {

                    LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

                    LocalDate endOfWeek = startOfWeek.plusDays(6);

                    fromDateTime = startOfWeek.atStartOfDay();

                    toDateTime = endOfWeek.atTime(LocalTime.MAX);
                }

                case THIS_MONTH -> {

                    LocalDate startOfMonth = today.withDayOfMonth(1);

                    LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

                    fromDateTime = startOfMonth.atStartOfDay();

                    toDateTime = endOfMonth.atTime(LocalTime.MAX);
                }
            }
        }

        // =========================================================
        // CUSTOM DATE RANGE
        // =========================================================

        if (period == null && fromDate != null && toDate != null) {

            fromDateTime = fromDate.atStartOfDay();

            toDateTime = toDate.atTime(LocalTime.MAX);
        }

        // =========================================================
        // VALIDATION
        // =========================================================

        if (period != null && (fromDate != null || toDate != null)) {

            throw new RuntimeException("Use either period or fromDate/toDate");
        }

        if ((fromDate != null && toDate == null) || (fromDate == null && toDate != null)) {

            throw new RuntimeException("Both fromDate and toDate are required");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {

            throw new RuntimeException("fromDate cannot be after toDate");
        }

        // =========================================================
        // CLIENT ID SEARCH
        // =========================================================

        if (clientId != null) {

            Optional<ClientAssignment> optional = clientAssignmentRepository.findByClientIdAndEmployeeAndActiveTrue(clientId, employee);

            if (optional.isEmpty()) {

                return Page.empty(pageable);
            }

            ClientAssignment assignment = optional.get();

            // Date filter against assignedAt
            if (fromDateTime != null) {

                LocalDateTime assignedAt = assignment.getAssignedAt();

                if (assignedAt == null || assignedAt.isBefore(fromDateTime) || assignedAt.isAfter(toDateTime)) {

                    return Page.empty(pageable);
                }
            }

            // Name filter with client ID
            if (name != null && !name.trim().isEmpty()) {

                String clientName = assignment.getClient().getName();

                if (clientName == null || !clientName.toLowerCase().contains(name.trim().toLowerCase())) {

                    return Page.empty(pageable);
                }
            }

            return new PageImpl<>(List.of(convertToDto(assignment)), pageable, 1);
        }

        // =========================================================
        // NAME + DATE
        // =========================================================

        if (name != null && !name.trim().isEmpty()) {

            if (fromDateTime != null) {

                return clientAssignmentRepository.findByEmployeeAndActiveTrueAndClient_NameContainingIgnoreCaseAndAssignedAtBetween(employee, name.trim(), fromDateTime, toDateTime, pageable).map(this::convertToDto);
            }

            return clientAssignmentRepository.findByEmployeeAndActiveTrueAndClient_NameContainingIgnoreCase(employee, name.trim(), pageable).map(this::convertToDto);
        }

        // =========================================================
        // DATE ONLY
        // =========================================================

        if (fromDateTime != null) {

            return clientAssignmentRepository.findByEmployeeAndActiveTrueAndAssignedAtBetween(employee, fromDateTime, toDateTime, pageable).map(this::convertToDto);
        }

        // =========================================================
        // NO FILTER
        // =========================================================

        return clientAssignmentRepository.findByEmployeeAndActiveTrueOrderByAssignedAtDesc(employee, pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<DocDocumentResponseDto> getClientDocuments(Long clientId, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        // =========================================================
        // VERIFY CLIENT IS ASSIGNED TO THIS EMPLOYEE
        // =========================================================

        ClientAssignment assignment = clientAssignmentRepository.findByClientIdAndEmployeeAndActiveTrue(clientId, employee).orElseThrow(() -> new RuntimeException("Client is not assigned to you"));

        // =========================================================
        // GET DOCUMENTS
        // =========================================================

        List<ClientDocument> documents = clientDocumentRepository.findByClientIdOrderByUpdatedAtDesc(clientId);

        return documents.stream().map(this::mapDocDocument).toList();
    }

    private DocDocumentResponseDto mapDocDocument(ClientDocument document) {

        User reviewer = document.getReviewedBy();

        return DocDocumentResponseDto.builder()

                .documentId(document.getId())

                .clientId(document.getClient().getId())

                .documentType(document.getDocumentType())

                .fileName(document.getFileName())

                .contentType(document.getContentType())

                .fileSize(document.getFileSize())

                .status(document.getStatus())

                // ================================================
                // ADMIN REVIEW DETAILS
                // ================================================

                .reviewComment(document.getReviewComment())

                .reviewedById(reviewer == null ? null : reviewer.getId())

                .reviewedByName(reviewer == null ? null : reviewer.getFirstName() + " " + reviewer.getLastName())

                .reviewedAt(document.getReviewedAt())

                .uploadedAt(document.getUploadedAt())

                .updatedAt(document.getUpdatedAt())

                .viewUrl(document.getFilePath() == null ? null : "/api/doc/documents/" + document.getId() + "/view")

                .downloadUrl(document.getFilePath() == null ? null : "/api/doc/documents/" + document.getId() + "/download")

                .build();
    }


}