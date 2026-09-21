package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.AssignmentPeriod;
import com.crm.matrix.enums.CallStatus;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.enums.DocumentStatus;
import com.crm.matrix.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
//@Transactional
public class AdminCRMService {

    private final ClientRepository clientRepository;
    private final ClientAssignmentRepository assignmentRepository;
    private final CallRecordRepository callRecordRepository;
    private final ClientCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final NotificationService notificationService;


    public ClientImportResponseDto uploadExcel(MultipartFile file, Authentication authentication) {

        System.out.println("===== START EXCEL IMPORT =====");

        try {

            // -----------------------------------------
            // 1. CHECK FILE
            // -----------------------------------------

            if (file == null) {
                throw new RuntimeException("MultipartFile is NULL");
            }

            System.out.println("File name: " + file.getOriginalFilename());

            System.out.println("File size: " + file.getSize());

            if (file.isEmpty()) {
                throw new RuntimeException("Uploaded Excel file is empty");
            }

            // -----------------------------------------
            // 2. CHECK AUTHENTICATION
            // -----------------------------------------

            if (authentication == null) {
                throw new RuntimeException("Authentication is NULL");
            }

            System.out.println("Logged user: " + authentication.getName());

            // -----------------------------------------
            // 3. GET ADMIN
            // -----------------------------------------

            User admin = getAdmin(authentication);

            System.out.println("Admin found: " + admin.getEmployeeCode());

            // -----------------------------------------
            // 4. CHECK FILE TYPE
            // -----------------------------------------

            String fileName = file.getOriginalFilename();

            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {

                throw new RuntimeException("Only .xlsx or .xls files are allowed");
            }

            int totalRows = 0;
            int successful = 0;
            int duplicates = 0;
            int invalidRows = 0;

            // -----------------------------------------
            // 5. READ EXCEL
            // -----------------------------------------

            try (InputStream inputStream = file.getInputStream();

                 Workbook workbook = WorkbookFactory.create(inputStream)) {

                System.out.println("Excel workbook opened successfully");

                Sheet sheet = workbook.getSheetAt(0);

                System.out.println("Sheet name: " + sheet.getSheetName());

                System.out.println("Total Excel rows: " + sheet.getLastRowNum());

                // -----------------------------------------
                // 6. READ ROWS
                // -----------------------------------------

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                    Row row = sheet.getRow(i);

                    if (row == null) {
                        continue;
                    }

                    totalRows++;

                    System.out.println("Processing row: " + (i + 1));

                    String name = getCellValue(row.getCell(0));

                    String email = getCellValue(row.getCell(1));

                    String phone = getCellValue(row.getCell(2));

                    System.out.println("Name: " + name);

                    System.out.println("Email: " + email);

                    System.out.println("Phone: " + phone);

                    // -----------------------------------------
                    // 7. VALIDATION
                    // -----------------------------------------

                    if (name == null || name.isBlank() || phone == null || phone.isBlank()) {

                        System.out.println("INVALID ROW");

                        invalidRows++;

                        continue;
                    }

                    // -----------------------------------------
                    // 8. NORMALIZE PHONE
                    // -----------------------------------------

                    phone = phone.replaceAll("[^0-9]", "");

                    if (phone.length() != 10) {

                        System.out.println("INVALID PHONE: " + phone);

                        invalidRows++;

                        continue;
                    }

                    // -----------------------------------------
                    // 9. NORMALIZE EMAIL
                    // -----------------------------------------

                    if (email != null && !email.isBlank()) {

                        email = email.trim().toLowerCase();
                    }

                    // -----------------------------------------
                    // 10. DUPLICATE CHECK
                    // -----------------------------------------

                    System.out.println("Checking phone duplicate...");

                    boolean duplicate = clientRepository.existsByPhone(phone);

                    if (!duplicate && email != null && !email.isBlank()) {

                        System.out.println("Checking email duplicate...");

                        duplicate = clientRepository.existsByEmail(email);
                    }

                    if (duplicate) {

                        System.out.println("DUPLICATE CLIENT");

                        duplicates++;

                        continue;
                    }

                    // -----------------------------------------
                    // 11. CREATE CLIENT
                    // -----------------------------------------

                    Client client = new Client();

                    client.setName(name.trim());

                    client.setEmail(email);

                    client.setPhone(phone);

                    client.setCreatedAt(LocalDateTime.now());

                    client.setUpdatedAt(LocalDateTime.now());

                    // -----------------------------------------
                    // 12. SAVE CLIENT
                    // -----------------------------------------

                    System.out.println("Saving client...");

                    clientRepository.save(client);

                    successful++;

                    System.out.println("CLIENT SAVED: " + client.getId());
                }
            }

            // -----------------------------------------
            // 13. RESPONSE
            // -----------------------------------------

            System.out.println("===== EXCEL IMPORT COMPLETE =====");

            System.out.println("Total: " + totalRows);

            System.out.println("Success: " + successful);

            System.out.println("Duplicates: " + duplicates);

            System.out.println("Invalid: " + invalidRows);

            return ClientImportResponseDto.builder().totalRows(totalRows).successful(successful).duplicates(duplicates).invalidRows(invalidRows).message("Excel uploaded successfully").build();

        } catch (Exception e) {

            System.err.println("===== EXCEL IMPORT FAILED =====");

            e.printStackTrace();

            throw new RuntimeException("Excel upload failed: " + e.getMessage(), e);
        }
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();

        String value = formatter.formatCellValue(cell);

        return value == null ? null : value.trim();
    }

    @Transactional
    public List<AssignmentResponseDto> bulkReassign(BulkReassignClientRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        User newEmployee = userRepository.findById(request.getNewEmployeeId()).orElseThrow(() -> new RuntimeException("Employee not found"));

        List<AssignmentResponseDto> response = new ArrayList<>();

        for (Long assignmentId : request.getAssignmentIds()) {

            ClientAssignment oldAssignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

            // Only active assignments can be reassigned

            if (!Boolean.TRUE.equals(oldAssignment.getActive())) {

                continue;
            }

            // -------------------------
            // CLOSE OLD ASSIGNMENT
            // -------------------------

            oldAssignment.setActive(false);

            oldAssignment.setEndedAt(LocalDateTime.now());

            assignmentRepository.save(oldAssignment);

            // -------------------------
            // CREATE NEW ASSIGNMENT
            // -------------------------

            ClientAssignment newAssignment = new ClientAssignment();

            newAssignment.setClient(oldAssignment.getClient());

            newAssignment.setEmployee(newEmployee);

            newAssignment.setAssignedBy(admin);

            newAssignment.setAssignedAt(LocalDateTime.now());

            newAssignment.setActive(true);

            newAssignment.setAssignmentReason(request.getReason());

            newAssignment.setCallInProgress(false);

            newAssignment = assignmentRepository.save(newAssignment);

            response.add(mapAssignment(newAssignment));
        }
        if (response.size() == 1) {
            notificationService.sendNotification(newEmployee, "Client Reassigned", "Client " + response.get(0).getClientName() + " has been reassigned to you.", "ASSIGNMENT","/clients");
        } else if (response.size() > 1) {
            notificationService.sendNotification(newEmployee, "Bulk Reassignment", response.size() + " clients have been reassigned to you.", "ASSIGNMENT","/clients");
        }

        return response;
    }

    private User getAdmin(Authentication authentication) {

        return userRepository.findByEmployeeCode(authentication.getName()).orElseThrow(() -> new RuntimeException("Admin not found"));
    }



    @Transactional(readOnly = true)
    public Page<AdminClientResponseDto> getClients(String stage, Pageable pageable) {

        // If a stage (e.g., DOC, PREP, NEW) is provided, filter by it
        if (stage != null && !stage.trim().isEmpty()) {
            return assignmentRepository.findActiveAssignmentsByClientStage(stage.trim(), pageable)
                    .map(this::mapAssignmentToClientResponse); // Uses your existing mapping[cite: 2]
        }

        // Otherwise, return all active assignments[cite: 2]
        return assignmentRepository.findAllActiveAssignmentsWithDetails(pageable)
                .map(this::mapAssignmentToClientResponse);
    }
    private AdminClientResponseDto mapAssignmentToClientResponse(ClientAssignment assignment) {
        Client client = assignment.getClient();
        User employee = assignment.getEmployee();

        return AdminClientResponseDto.builder()
                .clientId(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .status(client.getStatus())
                .currentStage(client.getCurrentStage())
                .nextFollowUpAt(client.getNextFollowUpAt())
                .assignedEmployeeId(employee != null ? employee.getId() : null)
                .assignedEmployeeName(employee != null ? employee.getFirstName() + " " + employee.getLastName() : null)
                .assignedAt(assignment.getAssignedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminClientSearchResponseDto> searchClients(Long clientId, String name, AssignmentPeriod period, LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        // =========================================================
        // PERIOD
        // =========================================================

        if (period != null) {

            LocalDate today = LocalDate.now();

            switch (period) {

                case TODAY -> {

                    fromDateTime = today.atStartOfDay();

                    toDateTime = today.atTime(LocalTime.MAX);
                }

                case THIS_WEEK -> {

                    LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);

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

            ClientAssignment assignment = assignmentRepository.findByClientIdAndActiveTrue(clientId).orElse(null);

            if (assignment == null) {

                return Page.empty(pageable);
            }

            if (fromDateTime != null) {

                LocalDateTime assignedAt = assignment.getAssignedAt();

                if (assignedAt == null || assignedAt.isBefore(fromDateTime) || assignedAt.isAfter(toDateTime)) {

                    return Page.empty(pageable);
                }
            }

            return new PageImpl<>(List.of(mapClientSearch(assignment.getClient())), pageable, 1);
        }


        // =========================================================
        // NAME + DATE
        // =========================================================

        if (name != null && !name.trim().isEmpty()) {

            if (fromDateTime != null) {

                return assignmentRepository.findActiveAssignmentsByClientNameBetween(name.trim(), fromDateTime, toDateTime, pageable).map(a -> mapClientSearch(a.getClient()));
            }

            return clientRepository.findByNameContainingIgnoreCase(name.trim(), pageable).map(this::mapClientSearch);
        }


        // =========================================================
        // DATE ONLY
        // =========================================================

        if (fromDateTime != null) {

            return assignmentRepository.findActiveAssignmentsBetween(fromDateTime, toDateTime, pageable).map(a -> mapClientSearch(a.getClient()));
        }


        // =========================================================
        // NO FILTER
        // =========================================================

        return clientRepository.findAll(pageable).map(this::mapClientSearch);
    }

    private AdminClientSearchResponseDto mapClientSearch(Client client) {

        ClientAssignment assignment = assignmentRepository.findByClientIdAndActiveTrue(client.getId()).orElse(null);

        List<CommentResponseDto> comments = commentRepository.findByClientIdAndDeletedFalseOrderByCreatedAtDesc(client.getId()).stream().map(this::mapComment).toList();

        return AdminClientSearchResponseDto.builder()

                .clientId(client.getId())

                .name(client.getName())

                .email(client.getEmail())

                .phone(client.getPhone())

                .status(client.getStatus())

                .currentStage(client.getCurrentStage())

                .nextFollowUpAt(client.getNextFollowUpAt())

                .assignedEmployeeId(assignment == null ? null : assignment.getEmployee().getId())

                .assignedEmployeeName(assignment == null ? null : assignment.getEmployee().getFirstName() + " " + assignment.getEmployee().getLastName())

                .assignedAt(assignment == null ? null : assignment.getAssignedAt())

                .comments(comments)

                .build();
    }

    // =========================================================
    // GET CLIENT
    // =========================================================

    @Transactional(readOnly = true)
    public AdminClientResponseDto getClient(Long clientId) {

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));

        return mapClient(client);
    }

    // =========================================================
    // BULK ASSIGN
    // =========================================================

    @Transactional
    public List<AssignmentResponseDto> bulkAssign(BulkAssignClientRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        User employee = userRepository.findById(request.getEmployeeId()).orElseThrow(() -> new RuntimeException("Employee not found"));

        List<AssignmentResponseDto> result = new java.util.ArrayList<>();

        for (Long clientId : request.getClientIds()) {

            Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

            // Deactivate previous assignment

            assignmentRepository.findByClientIdAndActiveTrue(clientId).ifPresent(old -> {

                old.setActive(false);
                old.setEndedAt(LocalDateTime.now());

                assignmentRepository.save(old);
            });

            // Create new assignment

            ClientAssignment assignment = new ClientAssignment();

            assignment.setClient(client);
            assignment.setEmployee(employee);
            assignment.setAssignedBy(admin);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setActive(true);
            assignment.setAssignmentReason(request.getReason());
            assignment.setCallInProgress(false);

            assignment = assignmentRepository.save(assignment);

            result.add(mapAssignment(assignment));
        }
        if (result.size() == 1) {
            notificationService.sendNotification(employee, "New Client Assigned", "Client " + result.get(0).getClientName() + " has been assigned to you.", "ASSIGNMENT","/clients");
        } else if (result.size() > 1) {
            notificationService.sendNotification(employee, "Bulk Assignment", result.size() + " new clients have been assigned to you.", "ASSIGNMENT","/clients");
        }

        return result;
    }

    // =========================================================
    // REASSIGN
    // =========================================================

    @Transactional
    public AssignmentResponseDto reassign(Long assignmentId, ReassignClientRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        ClientAssignment oldAssignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new RuntimeException("Assignment not found"));

        User newEmployee = userRepository.findById(request.getNewEmployeeId()).orElseThrow(() -> new RuntimeException("Employee not found"));

        // Close old assignment

        oldAssignment.setActive(false);

        oldAssignment.setEndedAt(LocalDateTime.now());

        assignmentRepository.save(oldAssignment);

        // Create new assignment

        ClientAssignment newAssignment = new ClientAssignment();

        newAssignment.setClient(oldAssignment.getClient());

        newAssignment.setEmployee(newEmployee);

        newAssignment.setAssignedBy(admin);

        newAssignment.setAssignedAt(LocalDateTime.now());

        newAssignment.setActive(true);

        newAssignment.setAssignmentReason(request.getReason());

        newAssignment.setCallInProgress(false);

        newAssignment = assignmentRepository.save(newAssignment);
        notificationService.sendNotification(newEmployee, "Client Reassigned", "Client " + oldAssignment.getClient().getName() + " has been reassigned to you.", "ASSIGNMENT","/clients");

        return mapAssignment(newAssignment);
    }

    // =========================================================
    // GET FOLLOW UPS
    // =========================================================

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getFollowUps() {

        return assignmentRepository.findActiveFollowUps().stream().map(this::mapAssignment).toList();
    }

    // =========================================================
    // GET NOT LIFTED
    // =========================================================

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getNotLifted() {

        return assignmentRepository.findActiveNotLifted().stream().map(this::mapAssignment).toList();
    }

    // =========================================================
    // GET CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminCallResponseDto> getCalls(Pageable pageable) {

        return callRecordRepository.findAllByOrderByStartedAtDesc(pageable).map(this::mapCall);
    }

    // =========================================================
    // GET CLIENT CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminCallResponseDto> getClientCalls(Long clientId, Pageable pageable) {

        return callRecordRepository.findByClientId(clientId, pageable).map(this::mapCall);
    }

    // =========================================================
    // GET RECORDING
    // =========================================================

    @Transactional(readOnly = true)
    public AdminCallResponseDto getRecording(Long callId) {

        CallRecord call = callRecordRepository.findById(callId).orElseThrow(() -> new RuntimeException("Call not found"));

        return mapCall(call);
    }

    // =========================================================
    // GET COMMENTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getClientComments(Long clientId) {

        return commentRepository.findByClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId).stream().map(this::mapComment).toList();
    }

    // =========================================================
    // DELETE COMMENT - SOFT DELETE
    // =========================================================

    @Transactional
    public void deleteComment(Long commentId, Authentication authentication) {

        User admin = getAdmin(authentication);

        ClientComment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Already deleted
        if (Boolean.TRUE.equals(comment.getDeleted())) {
            throw new RuntimeException("Comment is already deleted");
        }

        // Soft delete
        comment.setDeleted(true);

        comment.setDeletedAt(LocalDateTime.now());

        comment.setDeletedBy(admin);

        commentRepository.save(comment);
    }

    // =========================================================
    // EMPLOYEE CALL REPORT
    // =========================================================

    @Transactional(readOnly = true)
    public EmployeeCallReportDto getEmployeeReport(Long employeeId, LocalDate from, LocalDate to) {

        User employee = userRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));

        List<ClientAssignment> assignments = assignmentRepository.findByEmployeeIdAndActiveTrue(employeeId);

        LocalDateTime fromDateTime = from.atStartOfDay();

        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        List<CallRecord> calls = callRecordRepository.findByEmployeeIdAndStartedAtBetween(employeeId, fromDateTime, toDateTime);

        long callsMade = calls.size();

        long answered = calls.stream().filter(c -> c.getCallStatus() == CallStatus.ANSWERED || c.getCallStatus() == CallStatus.COMPLETED).count();

        long notLifted = calls.stream().filter(c -> c.getCallStatus() == CallStatus.NOT_LIFTED).count();

        long totalSeconds = calls.stream().mapToLong(c -> c.getDurationSeconds() == null ? 0 : c.getDurationSeconds()).sum();

        long followUps = assignments.stream().filter(a -> a.getClient().getStatus() == ClientStatus.FOLLOW_UP).count();

        long interested = assignments.stream().filter(a -> a.getClient().getStatus() == ClientStatus.INTERESTED).count();

        long notInterested = assignments.stream().filter(a -> a.getClient().getStatus() == ClientStatus.NOT_INTERESTED).count();

        long average = callsMade == 0 ? 0 : totalSeconds / callsMade;

        return EmployeeCallReportDto.builder()

                .employeeId(employee.getId())

                .employeeCode(employee.getEmployeeCode())

                .employeeName(employee.getFirstName() + " " + employee.getLastName())

                .assignedClients(assignments.size())

                .callsMade(callsMade)

                .answeredCalls(answered)

                .notLiftedCalls(notLifted)

                .followUps(followUps)

                .interested(interested)

                .notInterested(notInterested)

                .totalTalkTimeSeconds(totalSeconds)

                .totalTalkTimeMinutes(totalSeconds / 60)

                .averageCallSeconds(average)

                .build();
    }

    // =========================================================
    // MAPPERS
    // =========================================================
    private AdminClientResponseDto mapClient(Client client) {
        // 1. Fetch ALL assignments for this client (both active and inactive history)
        List<ClientAssignment> allAssignments = assignmentRepository.findByClientIdOrderByAssignedAtDesc(client.getId());

        // 2. Find the currently active assignment for the main table row display
        ClientAssignment activeAssignment = allAssignments.stream().filter(a -> Boolean.TRUE.equals(a.getActive())).findFirst().orElse(null);

        // 3. Map the full history using your existing mapAssignment method
        List<AssignmentResponseDto> history = allAssignments.stream().map(this::mapAssignment).collect(java.util.stream.Collectors.toList());

        return AdminClientResponseDto.builder().clientId(client.getId()).name(client.getName()).email(client.getEmail()).phone(client.getPhone()).status(client.getStatus()).currentStage(client.getCurrentStage()).nextFollowUpAt(client.getNextFollowUpAt()).assignedEmployeeId(activeAssignment == null ? null : activeAssignment.getEmployee().getId()).assignedEmployeeName(activeAssignment == null ? null : activeAssignment.getEmployee().getFirstName() + " " + activeAssignment.getEmployee().getLastName()).assignedAt(activeAssignment == null ? null : activeAssignment.getAssignedAt()).assignmentHistory(history) // Inject the full history list here
                .build();
    }

    private AssignmentResponseDto mapAssignment(ClientAssignment assignment) {
        User employee = assignment.getEmployee();
        String deptName = (employee.getDepartment() != null) ? employee.getDepartment().getName() : null;

        return AssignmentResponseDto.builder().assignmentId(assignment.getId()).clientId(assignment.getClient().getId()).clientName(assignment.getClient().getName()).employeeId(employee.getId()).employeeCode(employee.getEmployeeCode()).employeeName(employee.getFirstName() + " " + employee.getLastName()).departmentName(deptName) // MAP IT HERE
                .active(assignment.getActive()).assignedAt(assignment.getAssignedAt()).endedAt(assignment.getEndedAt()).reason(assignment.getAssignmentReason()).build();
    }

    private AdminCallResponseDto mapCall(CallRecord call) {

        User employee = call.getEmployee();

        return AdminCallResponseDto.builder()

                .callId(call.getId())

                .clientId(call.getClient().getId())

                .clientName(call.getClient().getName())

                .employeeId(employee.getId())

                .employeeName(employee.getFirstName() + " " + employee.getLastName())

                .provider(call.getProvider())

                .status(call.getCallStatus())

                .startedAt(call.getStartedAt())

                .endedAt(call.getEndedAt())

                .durationSeconds(call.getDurationSeconds())

                .recordingUrl(call.getRecordingUrl())

                .build();
    }

    @Transactional
    public AdminClientResponseDto updateClientStatus(Long clientId, UpdateClientStatusDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));

        client.setStatus(request.getStatus());

        client.setUpdatedAt(LocalDateTime.now());

        clientRepository.save(client);

        return mapClient(client);
    }

    @Transactional
    public AdminCommentResponseDto editComment(Long commentId, UpdateCommentRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        ClientComment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        if (Boolean.TRUE.equals(comment.getDeleted())) {
            throw new RuntimeException("Cannot edit deleted comment");
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

        response.setEmployeeId(saved.getEmployee().getId());

        response.setEmployeeName(saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName());

        response.setClientId(saved.getClient().getId());

        return response;
    }

    @Transactional(readOnly = true)
    public List<AdminClientResponseDto> getClientsByStatus(ClientStatus status) {

        return clientRepository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::mapClient).toList();
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

    @Transactional(readOnly = true)
    public Page<AdminDocumentResponseDto> getClientDocuments(Long clientId, Pageable pageable) {

        if (!clientRepository.existsById(clientId)) {
            throw new RuntimeException("Client not found");
        }

        return clientDocumentRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable).map(document -> mapAdminDocument(document, document.getClient()));
    }

    @Transactional(readOnly = true)
    public AdminDocumentResponseDto getDocument(Long documentId) {

        ClientDocument document = clientDocumentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        return mapAdminDocument(document, document.getClient());
    }

    @Transactional(readOnly = true)
    public Page<AdminClientDocumentStatusDto> getPendingDocumentClients(Pageable pageable) {

        return clientDocumentRepository.findClientsWithPendingDocuments(pageable).map(this::mapClientDocumentStatus);
    }

    @Transactional(readOnly = true)
    public Page<AdminClientDocumentStatusDto> getSubmittedDocumentClients(Pageable pageable) {

        return clientDocumentRepository.findClientsWithSubmittedDocuments(pageable).map(this::mapClientDocumentStatus);
    }

    @Transactional(readOnly = true)
    public AdminDocumentSummaryDto getDocumentSummary() {

        List<Client> clients = clientRepository.findAll();

        long submittedClients = clients.stream().filter(client -> clientDocumentRepository.countByClientIdAndStatus(client.getId(), DocumentStatus.SUBMITTED) > 0).count();

        long pendingClients = clients.size() - submittedClients;

        long totalDocuments = clientDocumentRepository.count();

        long submittedDocuments = clientDocumentRepository.countByStatus(DocumentStatus.SUBMITTED);

        return AdminDocumentSummaryDto.builder().totalClients(clients.size()).submittedClients(submittedClients).pendingClients(pendingClients).totalDocuments(totalDocuments).submittedDocuments(submittedDocuments).build();
    }

    private AdminDocumentResponseDto mapAdminDocument(ClientDocument document, Client client) {

        User reviewer = document.getReviewedBy();

        return AdminDocumentResponseDto.builder()

                .documentId(document.getId())

                .clientId(client.getId())

                .clientName(client.getName())

                .documentType(document.getDocumentType())

                .fileName(document.getFileName())

                .contentType(document.getContentType())

                .fileSize(document.getFileSize())

                .status(document.getStatus())

                .reviewComment(document.getReviewComment())

                .reviewedById(reviewer == null ? null : reviewer.getId())

                .reviewedByName(reviewer == null ? null : reviewer.getFirstName() + " " + reviewer.getLastName())

                .reviewedAt(document.getReviewedAt())

                .fileUrl(document.getFilePath() == null ? null : "/api/admin/documents/" + document.getId() + "/view")

                .uploadedAt(document.getUploadedAt())

                .updatedAt(document.getUpdatedAt())

                .build();
    }

    private AdminClientDocumentStatusDto mapClientDocumentStatus(Client client) {

        long totalDocuments = clientDocumentRepository.countByClientId(client.getId());

        long submittedDocuments = clientDocumentRepository.countByClientIdAndStatus(client.getId(), DocumentStatus.SUBMITTED);

        long pendingDocuments = totalDocuments - submittedDocuments;

        String documentStatus;

        if (submittedDocuments == 0) {

            documentStatus = "PENDING";

        } else if (pendingDocuments > 0) {

            documentStatus = "PARTIALLY_SUBMITTED";

        } else {

            documentStatus = "SUBMITTED";
        }

        return AdminClientDocumentStatusDto.builder()

                .clientId(client.getId())

                .name(client.getName())

                .email(client.getEmail())

                .phone(client.getPhone())

                .totalDocuments(totalDocuments)

                .submittedDocuments(submittedDocuments)

                .pendingDocuments(pendingDocuments)

                .documentStatus(documentStatus)

                .build();
    }

    @Transactional
    public List<AdminClientResponseDto> bulkAssignToPreparation(BulkAssignPrepRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);
        User prepEmployee = userRepository.findById(request.getPrepEmployeeId()).orElseThrow(() -> new RuntimeException("Prep Employee not found"));

        if (prepEmployee.getDepartment() == null || !prepEmployee.getDepartment().getName().equalsIgnoreCase("PREPARATION")) {
            throw new RuntimeException("You can only assign these clients to an employee in the Preparation team.");
        }

        List<AdminClientResponseDto> response = new java.util.ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

            // ========================================================
            // 1. SECURITY GATE: STRICT DOCUMENT APPROVAL RULE
            // ========================================================
            long totalDocs = clientDocumentRepository.countByClientId(clientId);
            long verifiedDocs = clientDocumentRepository.countByClientIdAndStatus(clientId, DocumentStatus.VERIFIED);

            // Fail if they have 0 documents, OR if the verified count doesn't match the total count
            if (totalDocs == 0 || verifiedDocs != totalDocs) {
                throw new RuntimeException("Cannot assign client " + client.getName() + " to Preparation. They must have at least one document and ALL documents must be VERIFIED.");
            }
            // ========================================================

            // 2. Advance the Stage and Status
            client.setCurrentStage("PREP");
            client.setStatus(ClientStatus.PREPARATION_ASSIGNED);
            client.setUpdatedAt(LocalDateTime.now());
            clientRepository.save(client);

            // 3. Close previous assignment (from the Doc team)
            assignmentRepository.findByClientIdAndActiveTrue(clientId).ifPresent(old -> {
                old.setActive(false);
                old.setEndedAt(LocalDateTime.now());
                assignmentRepository.save(old);
            });

            // 4. Create the new assignment for the Prep Employee
            ClientAssignment newAssignment = new ClientAssignment();
            newAssignment.setClient(client);
            newAssignment.setEmployee(prepEmployee);
            newAssignment.setAssignedBy(admin);
            newAssignment.setAssignedAt(LocalDateTime.now());
            newAssignment.setActive(true);
            newAssignment.setAssignmentReason("Bulk Assigned to Tax Preparation");
            newAssignment.setCallInProgress(false);
            assignmentRepository.save(newAssignment);

            response.add(mapClient(client));
        }

        if (response.size() == 1) {
            notificationService.sendNotification(prepEmployee, "New Prep Client", "Client " + response.get(0).getName() + " is ready for tax preparation.", "ASSIGNMENT","/clients");
        } else if (response.size() > 1) {
            notificationService.sendNotification(prepEmployee, "Bulk Prep Assignment", response.size() + " clients are now ready for tax preparation.", "ASSIGNMENT","/clients");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Page<AdminClientResponseDto> getClientsByEmployee(Long employeeId, Pageable pageable) {
        User employee = userRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));

        // Fetch all assignments (both active and past/transferred) for this employee
        return assignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(employee.getId(), pageable).map(assignment -> mapClient(assignment.getClient()));
    }

    @Transactional
    public List<AdminDocumentResponseDto> approveAllDocuments(Long clientId, Authentication authentication) {

        User admin = getAdmin(authentication);

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        List<ClientDocument> documents = clientDocumentRepository.findByClientIdAndStatus(clientId, DocumentStatus.SUBMITTED);

        if (documents.isEmpty()) {
            throw new RuntimeException("No SUBMITTED documents found for client " + clientId);
        }

        LocalDateTime now = LocalDateTime.now();

        for (ClientDocument document : documents) {

            document.setStatus(DocumentStatus.VERIFIED);

            document.setReviewComment(null);

            document.setReviewedBy(admin);

            document.setReviewedAt(now);

            document.setUpdatedAt(now);
        }

        clientDocumentRepository.saveAll(documents);

        return documents.stream().map(document -> mapAdminDocument(document, client)).toList();
    }

    @Transactional
    public List<AdminDocumentResponseDto> rejectAllDocuments(Long clientId, DocumentReviewRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {

            throw new RuntimeException("Rejection comment is required");
        }

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        List<ClientDocument> documents = clientDocumentRepository.findByClientIdAndStatus(clientId, DocumentStatus.SUBMITTED);

        if (documents.isEmpty()) {
            throw new RuntimeException("No SUBMITTED documents found for client " + clientId);
        }

        LocalDateTime now = LocalDateTime.now();

        String comment = request.getComment().trim();

        for (ClientDocument document : documents) {

            document.setStatus(DocumentStatus.REJECTED);

            document.setReviewComment(comment);

            document.setReviewedBy(admin);

            document.setReviewedAt(now);

            document.setUpdatedAt(now);
        }

        clientDocumentRepository.saveAll(documents);

        return documents.stream().map(document -> mapAdminDocument(document, client)).toList();
    }

    @Transactional
    public AdminDocumentResponseDto approveDocument(Long clientId, Long documentId, Authentication authentication) {

        User admin = getAdmin(authentication);

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        ClientDocument document = clientDocumentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // IMPORTANT:
        // Make sure this document belongs to this client
        if (!document.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Document does not belong to client " + clientId);
        }

        if (document.getStatus() != DocumentStatus.SUBMITTED) {

            throw new RuntimeException("Only SUBMITTED documents can be approved");
        }

        document.setStatus(DocumentStatus.VERIFIED);

        document.setReviewComment(null);

        document.setReviewedBy(admin);

        document.setReviewedAt(LocalDateTime.now());

        document.setUpdatedAt(LocalDateTime.now());

        ClientDocument saved = clientDocumentRepository.save(document);

        return mapAdminDocument(saved, client);
    }

    @Transactional
    public AdminDocumentResponseDto rejectDocument(Long clientId, Long documentId, DocumentReviewRequestDto request, Authentication authentication) {

        User admin = getAdmin(authentication);

        if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {

            throw new RuntimeException("Rejection comment is required");
        }

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        ClientDocument document = clientDocumentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // IMPORTANT SECURITY CHECK
        if (!document.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Document does not belong to client " + clientId);
        }

        if (document.getStatus() != DocumentStatus.SUBMITTED) {

            throw new RuntimeException("Only SUBMITTED documents can be rejected");
        }

        document.setStatus(DocumentStatus.REJECTED);

        document.setReviewComment(request.getComment().trim());

        document.setReviewedBy(admin);

        document.setReviewedAt(LocalDateTime.now());

        document.setUpdatedAt(LocalDateTime.now());

        ClientDocument saved = clientDocumentRepository.save(document);

        return mapAdminDocument(saved, client);
    }


    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getClientAssignmentHistory(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new RuntimeException("Client not found");
        }

        List<ClientAssignment> assignments = assignmentRepository.findByClientIdOrderByAssignedAtDesc(clientId);

        return assignments.stream().map(this::mapAssignment).toList();
    }



    @Transactional(readOnly = true)
    public Page<AdminClientDocumentStatusDto> getVerifiedDocumentClients(Pageable pageable) {
        // Calling the new strict query
        return clientDocumentRepository.findClientsWithAllDocumentsVerified(pageable)
                .map(this::mapVerifiedClient);
    }

    private AdminClientDocumentStatusDto mapVerifiedClient(Client client) {
        long totalDocuments = clientDocumentRepository.countByClientId(client.getId());
        long verifiedDocuments = clientDocumentRepository.countByClientIdAndStatus(client.getId(), DocumentStatus.VERIFIED);
        long pendingDocuments = totalDocuments - verifiedDocuments;

        return AdminClientDocumentStatusDto.builder()
                .clientId(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .totalDocuments(totalDocuments)
                .submittedDocuments(verifiedDocuments)
                .pendingDocuments(pendingDocuments)
                .documentStatus("VERIFIED")
                .currentStage(client.getCurrentStage()) // <-- ఇక్కడ స్టేజ్ యాడ్ చేయబడింది
                .status(client.getStatus())             // <-- ఇక్కడ క్లయింట్ స్టేటస్ యాడ్ చేయబడింది
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminClientResponseDto> getUnassignedClients(Pageable pageable) {
        return clientRepository.findUnassignedClients(pageable)
                .map(this::mapClient); // Uses your existing mapping logic
    }
}