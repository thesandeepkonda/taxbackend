package com.crm.matrix.service;


import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.CallStatus;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
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
@Transactional
public class AdminCRMService {

    private final ClientRepository clientRepository;
    private final ClientAssignmentRepository assignmentRepository;
    private final CallRecordRepository callRecordRepository;
    private final ClientCommentRepository commentRepository;
    private final UserRepository userRepository;

    public ClientImportResponseDto uploadExcel(
            MultipartFile file,
            Authentication authentication) {

        System.out.println("===== START EXCEL IMPORT =====");

        try {

            // -----------------------------------------
            // 1. CHECK FILE
            // -----------------------------------------

            if (file == null) {
                throw new RuntimeException("MultipartFile is NULL");
            }

            System.out.println(
                    "File name: " + file.getOriginalFilename()
            );

            System.out.println(
                    "File size: " + file.getSize()
            );

            if (file.isEmpty()) {
                throw new RuntimeException(
                        "Uploaded Excel file is empty"
                );
            }


            // -----------------------------------------
            // 2. CHECK AUTHENTICATION
            // -----------------------------------------

            if (authentication == null) {
                throw new RuntimeException(
                        "Authentication is NULL"
                );
            }

            System.out.println(
                    "Logged user: "
                            + authentication.getName()
            );


            // -----------------------------------------
            // 3. GET ADMIN
            // -----------------------------------------

            User admin = getAdmin(authentication);

            System.out.println(
                    "Admin found: "
                            + admin.getEmployeeCode()
            );


            // -----------------------------------------
            // 4. CHECK FILE TYPE
            // -----------------------------------------

            String fileName =
                    file.getOriginalFilename();

            if (fileName == null ||
                    (!fileName.toLowerCase().endsWith(".xlsx")
                            && !fileName.toLowerCase().endsWith(".xls"))) {

                throw new RuntimeException(
                        "Only .xlsx or .xls files are allowed"
                );
            }


            int totalRows = 0;
            int successful = 0;
            int duplicates = 0;
            int invalidRows = 0;


            // -----------------------------------------
            // 5. READ EXCEL
            // -----------------------------------------

            try (
                    InputStream inputStream =
                            file.getInputStream();

                    Workbook workbook =
                            WorkbookFactory.create(
                                    inputStream
                            )
            ) {

                System.out.println(
                        "Excel workbook opened successfully"
                );

                Sheet sheet =
                        workbook.getSheetAt(0);

                System.out.println(
                        "Sheet name: "
                                + sheet.getSheetName()
                );

                System.out.println(
                        "Total Excel rows: "
                                + sheet.getLastRowNum()
                );


                // -----------------------------------------
                // 6. READ ROWS
                // -----------------------------------------

                for (
                        int i = 1;
                        i <= sheet.getLastRowNum();
                        i++
                ) {

                    Row row =
                            sheet.getRow(i);

                    if (row == null) {
                        continue;
                    }

                    totalRows++;

                    System.out.println(
                            "Processing row: "
                                    + (i + 1)
                    );


                    String name =
                            getCellValue(
                                    row.getCell(0)
                            );

                    String email =
                            getCellValue(
                                    row.getCell(1)
                            );

                    String phone =
                            getCellValue(
                                    row.getCell(2)
                            );


                    System.out.println(
                            "Name: " + name
                    );

                    System.out.println(
                            "Email: " + email
                    );

                    System.out.println(
                            "Phone: " + phone
                    );


                    // -----------------------------------------
                    // 7. VALIDATION
                    // -----------------------------------------

                    if (
                            name == null
                                    || name.isBlank()
                                    || phone == null
                                    || phone.isBlank()
                    ) {

                        System.out.println(
                                "INVALID ROW"
                        );

                        invalidRows++;

                        continue;
                    }


                    // -----------------------------------------
                    // 8. NORMALIZE PHONE
                    // -----------------------------------------

                    phone =
                            phone.replaceAll(
                                    "[^0-9]",
                                    ""
                            );

                    if (phone.length() != 10) {

                        System.out.println(
                                "INVALID PHONE: "
                                        + phone
                        );

                        invalidRows++;

                        continue;
                    }


                    // -----------------------------------------
                    // 9. NORMALIZE EMAIL
                    // -----------------------------------------

                    if (
                            email != null
                                    && !email.isBlank()
                    ) {

                        email =
                                email
                                        .trim()
                                        .toLowerCase();
                    }


                    // -----------------------------------------
                    // 10. DUPLICATE CHECK
                    // -----------------------------------------

                    System.out.println(
                            "Checking phone duplicate..."
                    );

                    boolean duplicate =
                            clientRepository
                                    .existsByPhone(phone);


                    if (
                            !duplicate
                                    && email != null
                                    && !email.isBlank()
                    ) {

                        System.out.println(
                                "Checking email duplicate..."
                        );

                        duplicate =
                                clientRepository
                                        .existsByEmail(
                                                email
                                        );
                    }


                    if (duplicate) {

                        System.out.println(
                                "DUPLICATE CLIENT"
                        );

                        duplicates++;

                        continue;
                    }


                    // -----------------------------------------
                    // 11. CREATE CLIENT
                    // -----------------------------------------

                    Client client =
                            new Client();

                    client.setName(
                            name.trim()
                    );

                    client.setEmail(
                            email
                    );

                    client.setPhone(
                            phone
                    );

                    client.setCreatedAt(
                            LocalDateTime.now()
                    );

                    client.setUpdatedAt(
                            LocalDateTime.now()
                    );


                    // -----------------------------------------
                    // 12. SAVE CLIENT
                    // -----------------------------------------

                    System.out.println(
                            "Saving client..."
                    );

                    clientRepository.save(client);

                    successful++;

                    System.out.println(
                            "CLIENT SAVED: "
                                    + client.getId()
                    );
                }
            }


            // -----------------------------------------
            // 13. RESPONSE
            // -----------------------------------------

            System.out.println(
                    "===== EXCEL IMPORT COMPLETE ====="
            );

            System.out.println(
                    "Total: " + totalRows
            );

            System.out.println(
                    "Success: " + successful
            );

            System.out.println(
                    "Duplicates: " + duplicates
            );

            System.out.println(
                    "Invalid: " + invalidRows
            );


            return ClientImportResponseDto
                    .builder()
                    .totalRows(totalRows)
                    .successful(successful)
                    .duplicates(duplicates)
                    .invalidRows(invalidRows)
                    .message(
                            "Excel uploaded successfully"
                    )
                    .build();


        } catch (Exception e) {

            System.err.println(
                    "===== EXCEL IMPORT FAILED ====="
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Excel upload failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter =
                new DataFormatter();

        String value =
                formatter.formatCellValue(cell);

        return value == null
                ? null
                : value.trim();
    }


    public List<AssignmentResponseDto> bulkReassign(
            BulkReassignClientRequestDto request,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        User newEmployee =
                userRepository.findById(
                        request.getNewEmployeeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found"
                        )
                );

        List<AssignmentResponseDto> response =
                new ArrayList<>();

        for (Long assignmentId :
                request.getAssignmentIds()) {

            ClientAssignment oldAssignment =
                    assignmentRepository.findById(
                            assignmentId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Assignment not found: "
                                            + assignmentId
                            )
                    );

            // Only active assignments can be reassigned

            if (!Boolean.TRUE.equals(
                    oldAssignment.getActive())) {

                continue;
            }

            // -------------------------
            // CLOSE OLD ASSIGNMENT
            // -------------------------

            oldAssignment.setActive(false);

            oldAssignment.setEndedAt(
                    LocalDateTime.now()
            );

            assignmentRepository.save(
                    oldAssignment
            );

            // -------------------------
            // CREATE NEW ASSIGNMENT
            // -------------------------

            ClientAssignment newAssignment =
                    new ClientAssignment();

            newAssignment.setClient(
                    oldAssignment.getClient()
            );

            newAssignment.setEmployee(
                    newEmployee
            );

            newAssignment.setAssignedBy(
                    admin
            );

            newAssignment.setAssignedAt(
                    LocalDateTime.now()
            );

            newAssignment.setActive(true);

            newAssignment.setAssignmentReason(
                    request.getReason()
            );

            newAssignment.setCallInProgress(
                    false
            );

            newAssignment =
                    assignmentRepository.save(
                            newAssignment
                    );

            response.add(
                    mapAssignment(newAssignment)
            );
        }

        return response;
    }

    private User getAdmin(Authentication authentication) {

        return userRepository
                .findByEmployeeCode(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Admin not found"));
    }


    // =========================================================
    // GET ALL CLIENTS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminClientResponseDto> getClients(
            Pageable pageable) {

        return clientRepository
                .findAll(pageable)
                .map(this::mapClient);
    }


    // =========================================================
    // SEARCH BY NAME
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminClientResponseDto> searchByName(
            String name,
            Pageable pageable) {

        return clientRepository
                .findByNameContainingIgnoreCase(
                        name,
                        pageable
                )
                .map(this::mapClient);
    }


    // =========================================================
    // GET CLIENT
    // =========================================================

    @Transactional(readOnly = true)
    public AdminClientResponseDto getClient(
            Long clientId) {

        Client client =
                clientRepository.findById(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client not found"
                                ));

        return mapClient(client);
    }


    // =========================================================
    // BULK ASSIGN
    // =========================================================

    public List<AssignmentResponseDto> bulkAssign(
            BulkAssignClientRequestDto request,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        User employee =
                userRepository.findById(
                        request.getEmployeeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found"
                        ));

        List<AssignmentResponseDto> result =
                new java.util.ArrayList<>();

        for (Long clientId :
                request.getClientIds()) {

            Client client =
                    clientRepository.findById(clientId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Client not found: "
                                                    + clientId
                                    ));

            // Deactivate previous assignment

            assignmentRepository
                    .findByClientIdAndActiveTrue(clientId)
                    .ifPresent(old -> {

                        old.setActive(false);
                        old.setEndedAt(
                                LocalDateTime.now()
                        );

                        assignmentRepository.save(old);
                    });


            // Create new assignment

            ClientAssignment assignment =
                    new ClientAssignment();

            assignment.setClient(client);
            assignment.setEmployee(employee);
            assignment.setAssignedBy(admin);
            assignment.setAssignedAt(
                    LocalDateTime.now()
            );
            assignment.setActive(true);
            assignment.setAssignmentReason(
                    request.getReason()
            );
            assignment.setCallInProgress(false);

            assignment =
                    assignmentRepository.save(
                            assignment
                    );

            result.add(
                    mapAssignment(assignment)
            );
        }

        return result;
    }


    // =========================================================
    // REASSIGN
    // =========================================================

    public AssignmentResponseDto reassign(
            Long assignmentId,
            ReassignClientRequestDto request,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        ClientAssignment oldAssignment =
                assignmentRepository.findById(
                        assignmentId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Assignment not found"
                        ));

        User newEmployee =
                userRepository.findById(
                        request.getNewEmployeeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found"
                        ));


        // Close old assignment

        oldAssignment.setActive(false);

        oldAssignment.setEndedAt(
                LocalDateTime.now()
        );

        assignmentRepository.save(
                oldAssignment
        );


        // Create new assignment

        ClientAssignment newAssignment =
                new ClientAssignment();

        newAssignment.setClient(
                oldAssignment.getClient()
        );

        newAssignment.setEmployee(
                newEmployee
        );

        newAssignment.setAssignedBy(
                admin
        );

        newAssignment.setAssignedAt(
                LocalDateTime.now()
        );

        newAssignment.setActive(true);

        newAssignment.setAssignmentReason(
                request.getReason()
        );

        newAssignment.setCallInProgress(false);

        newAssignment =
                assignmentRepository.save(
                        newAssignment
                );

        return mapAssignment(
                newAssignment
        );
    }


    // =========================================================
    // GET FOLLOW UPS
    // =========================================================

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getFollowUps() {

        return assignmentRepository
                .findActiveFollowUps()
                .stream()
                .map(this::mapAssignment)
                .toList();
    }


    // =========================================================
    // GET NOT LIFTED
    // =========================================================

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getNotLifted() {

        return assignmentRepository
                .findActiveNotLifted()
                .stream()
                .map(this::mapAssignment)
                .toList();
    }


    // =========================================================
    // GET CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminCallResponseDto> getCalls(
            Pageable pageable) {

        return callRecordRepository
                .findAllByOrderByStartedAtDesc(
                        pageable
                )
                .map(this::mapCall);
    }


    // =========================================================
    // GET CLIENT CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AdminCallResponseDto> getClientCalls(
            Long clientId,
            Pageable pageable) {

        return callRecordRepository
                .findByClientId(
                        clientId,
                        pageable
                )
                .map(this::mapCall);
    }


    // =========================================================
    // GET RECORDING
    // =========================================================

    @Transactional(readOnly = true)
    public AdminCallResponseDto getRecording(
            Long callId) {

        CallRecord call =
                callRecordRepository.findById(
                        callId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Call not found"
                        ));

        return mapCall(call);
    }


    // =========================================================
    // GET COMMENTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<CommentResponseDto>
    getClientComments(Long clientId) {

        return commentRepository
                .findByClientIdAndDeletedFalseOrderByCreatedAtDesc(
                        clientId
                )
                .stream()
                .map(this::mapComment)
                .toList();
    }





// =========================================================
// DELETE COMMENT - SOFT DELETE
// =========================================================

    public void deleteComment(
            Long commentId,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        ClientComment comment =
                commentRepository.findById(commentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Comment not found: " + commentId
                                ));

        // Already deleted
        if (Boolean.TRUE.equals(comment.getDeleted())) {
            throw new RuntimeException(
                    "Comment is already deleted"
            );
        }

        // Soft delete
        comment.setDeleted(true);

        comment.setDeletedAt(
                LocalDateTime.now()
        );

        comment.setDeletedBy(admin);

        commentRepository.save(comment);
    }




    // =========================================================
    // EMPLOYEE CALL REPORT
    // =========================================================

    @Transactional(readOnly = true)
    public EmployeeCallReportDto
    getEmployeeReport(
            Long employeeId,
            LocalDate from,
            LocalDate to) {

        User employee =
                userRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found"
                                ));

        List<ClientAssignment> assignments =
                assignmentRepository
                        .findByEmployeeIdAndActiveTrue(
                                employeeId
                        );

        LocalDateTime fromDateTime =
                from.atStartOfDay();

        LocalDateTime toDateTime =
                to.atTime(
                        LocalTime.MAX
                );

        List<CallRecord> calls =
                callRecordRepository
                        .findByEmployeeIdAndStartedAtBetween(
                                employeeId,
                                fromDateTime,
                                toDateTime
                        );

        long callsMade = calls.size();

        long answered =
                calls.stream()
                        .filter(c ->
                                c.getCallStatus()
                                        == CallStatus.ANSWERED
                                ||
                                c.getCallStatus()
                                        == CallStatus.COMPLETED
                        )
                        .count();

        long notLifted =
                calls.stream()
                        .filter(c ->
                                c.getCallStatus()
                                        == CallStatus.NOT_LIFTED
                        )
                        .count();

        long totalSeconds =
                calls.stream()
                        .mapToLong(c ->
                                c.getDurationSeconds() == null
                                        ? 0
                                        : c.getDurationSeconds()
                        )
                        .sum();

        long followUps =
                assignments.stream()
                        .filter(a ->
                                a.getClient()
                                        .getStatus()
                                        == ClientStatus.FOLLOW_UP
                        )
                        .count();

        long interested =
                assignments.stream()
                        .filter(a ->
                                a.getClient()
                                        .getStatus()
                                        == ClientStatus.INTERESTED
                        )
                        .count();

        long notInterested =
                assignments.stream()
                        .filter(a ->
                                a.getClient()
                                        .getStatus()
                                        == ClientStatus.NOT_INTERESTED
                        )
                        .count();

        long average =
                callsMade == 0
                        ? 0
                        : totalSeconds / callsMade;

        return EmployeeCallReportDto.builder()

                .employeeId(
                        employee.getId()
                )

                .employeeCode(
                        employee.getEmployeeCode()
                )

                .employeeName(
                        employee.getFirstName()
                                + " "
                                + employee.getLastName()
                )

                .assignedClients(
                        assignments.size()
                )

                .callsMade(
                        callsMade
                )

                .answeredCalls(
                        answered
                )

                .notLiftedCalls(
                        notLifted
                )

                .followUps(
                        followUps
                )

                .interested(
                        interested
                )

                .notInterested(
                        notInterested
                )

                .totalTalkTimeSeconds(
                        totalSeconds
                )

                .totalTalkTimeMinutes(
                        totalSeconds / 60
                )

                .averageCallSeconds(
                        average
                )

                .build();
    }


    // =========================================================
    // MAPPERS
    // =========================================================

    private AdminClientResponseDto mapClient(
            Client client) {

        ClientAssignment assignment =
                assignmentRepository
                        .findByClientIdAndActiveTrue(
                                client.getId()
                        )
                        .orElse(null);

        return AdminClientResponseDto.builder()

                .clientId(client.getId())

                .name(client.getName())

                .email(client.getEmail())

                .phone(client.getPhone())

                .status(client.getStatus())

                .currentStage(
                        client.getCurrentStage()
                )

                .nextFollowUpAt(
                        client.getNextFollowUpAt()
                )

                .assignedEmployeeId(
                        assignment == null
                                ? null
                                : assignment
                                    .getEmployee()
                                    .getId()
                )

                .assignedEmployeeName(
                        assignment == null
                                ? null
                                : assignment
                                    .getEmployee()
                                    .getFirstName()
                                    + " "
                                    + assignment
                                        .getEmployee()
                                        .getLastName()
                )

                .assignedAt(
                        assignment == null
                                ? null
                                : assignment.getAssignedAt()
                )

                .build();
    }


    private AssignmentResponseDto mapAssignment(
            ClientAssignment assignment) {

        User employee =
                assignment.getEmployee();

        return AssignmentResponseDto.builder()

                .assignmentId(
                        assignment.getId()
                )

                .clientId(
                        assignment.getClient().getId()
                )

                .clientName(
                        assignment.getClient().getName()
                )

                .employeeId(
                        employee.getId()
                )

                .employeeCode(
                        employee.getEmployeeCode()
                )

                .employeeName(
                        employee.getFirstName()
                                + " "
                                + employee.getLastName()
                )

                .active(
                        assignment.getActive()
                )

                .assignedAt(
                        assignment.getAssignedAt()
                )

                .endedAt(
                        assignment.getEndedAt()
                )

                .reason(
                        assignment.getAssignmentReason()
                )

                .build();
    }


    private AdminCallResponseDto mapCall(
            CallRecord call) {

        User employee =
                call.getEmployee();

        return AdminCallResponseDto.builder()

                .callId(call.getId())

                .clientId(
                        call.getClient().getId()
                )

                .clientName(
                        call.getClient().getName()
                )

                .employeeId(
                        employee.getId()
                )

                .employeeName(
                        employee.getFirstName()
                                + " "
                                + employee.getLastName()
                )

                .provider(
                        call.getProvider()
                )

                .status(
                        call.getCallStatus()
                )

                .startedAt(
                        call.getStartedAt()
                )

                .endedAt(
                        call.getEndedAt()
                )

                .durationSeconds(
                        call.getDurationSeconds()
                )

                .recordingUrl(
                        call.getRecordingUrl()
                )

                .build();
    }


    private CommentResponseDto mapComment(
            ClientComment comment) {

        User employee = comment.getEmployee();

        return CommentResponseDto.builder()

                .id(comment.getId())

                .clientId(
                        comment.getClient().getId()
                )

                .employeeId(
                        employee.getId()
                )

                .employeeName(
                        employee.getFirstName()
                                + " "
                                + employee.getLastName()
                )

                .comment(
                        comment.getComment()
                )

                .commentType(
                        comment.getCommentType()
                )

                .createdAt(
                        comment.getCreatedAt()
                )

                .build();
    }
}