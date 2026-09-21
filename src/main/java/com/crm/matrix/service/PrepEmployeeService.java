package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrepEmployeeService {

    private final UserRepository userRepository;
    private final ClientAssignmentRepository assignmentRepository;
    private final ClientRepository clientRepository;
    private final TaxDraftRepository taxDraftRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final ClientCommentRepository commentRepository;
    private final NotificationService notificationService;

    private final Path draftUploadDirectory = Paths.get("uploads/drafts");

    private User getLoggedInUser(Authentication authentication) {
        return userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    @Transactional(readOnly = true)
    public Page<PrepClientResponseDto> getMyPrepClients(Authentication authentication, Pageable pageable) {
        User employee = getLoggedInUser(authentication);
        List<ClientStatus> activeStatuses = List.of(
                ClientStatus.PREPARATION_ASSIGNED,
                ClientStatus.PREPARATION_IN_PROGRESS,
                ClientStatus.DRAFT_REJECTED
        );
        return assignmentRepository.findPrepAssignmentsByStatuses(employee, activeStatuses, pageable)
                .map(this::mapToPrepDto);
    }

    @Transactional(readOnly = true)
    public Page<PrepClientResponseDto> getReadyForReviewClients(Authentication authentication, Pageable pageable) {
        User employee = getLoggedInUser(authentication);
        List<ClientStatus> reviewStatuses = List.of(ClientStatus.DRAFT_READY);
        return assignmentRepository.findPrepAssignmentsByStatuses(employee, reviewStatuses, pageable)
                .map(this::mapToPrepDto);
    }

    private PrepClientResponseDto mapToPrepDto(ClientAssignment assignment) {
        Client client = assignment.getClient();

        List<ClientDocument> documents = clientDocumentRepository.findByClientIdOrderByUploadedAtDesc(client.getId());

        List<DocumentResponseDto> documentDtos = documents.stream()
                .map(doc -> DocumentResponseDto.builder()
                        .documentId(doc.getId())
                        .documentType(doc.getDocumentType())
                        .fileName(doc.getFileName())
                        .contentType(doc.getContentType())
                        .fileSize(doc.getFileSize())
                        .status(doc.getStatus())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        String assignedByName = "System";
        if (assignment.getAssignedBy() != null) {
            assignedByName = assignment.getAssignedBy().getFirstName();
            if (assignment.getAssignedBy().getLastName() != null) {
                assignedByName += " " + assignment.getAssignedBy().getLastName();
            }
        }

        return PrepClientResponseDto.builder()
                .assignmentId(assignment.getId())
                .clientId(client.getId())
                .clientName(client.getName())
                .status(client.getStatus())
                .assignedBy(assignedByName)
                .assignedAt(assignment.getAssignedAt())
                .documents(documentDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Resource getClientSourceDocument(ClientDocument document) {
        try {
            Path path = Paths.get(document.getFilePath())
                    .toAbsolutePath()
                    .normalize();

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found on server");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    @Transactional // <--- Moved here so the whole flow shares a session/transaction
    public PrepClientResponseDto submitDraft(Long assignmentId, MultipartFile file, String remarks, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Draft file is required");
        }

        // STEP 1: Validate assignment and calculate next version
        Long clientId = validateAndGetClientIdForPrep(assignmentId, authentication);
        int version = getNextDraftVersion(clientId);

        // STEP 2: Save to disk
        Path savedFilePath = saveDraftToDisk(clientId, version, file);

        // STEP 3: Save to DB and trigger notifications
        return finalizeDraftSubmissionInternal(assignmentId, version, file, savedFilePath, remarks, authentication);
    }

    // Remove @Transactional from here since submitDraft now handles the transaction boundary
    protected PrepClientResponseDto finalizeDraftSubmissionInternal(Long assignmentId, int version,
                                                                    MultipartFile file, Path filePath, String remarks,
                                                                    Authentication authentication) {
        User employee = getLoggedInUser(authentication);
        ClientAssignment assignment = assignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
                .orElseThrow(() -> new RuntimeException("Assignment not found or not active"));
        Client client = assignment.getClient();

        TaxDraft draft = new TaxDraft();
        draft.setClient(client);
        draft.setPrepEmployee(employee);
        draft.setDraftVersion(version);
        draft.setFileName(file.getOriginalFilename());
        draft.setFilePath(filePath.toAbsolutePath().toString());
        draft.setContentType(file.getContentType());
        draft.setPrepRemarks(remarks);
        draft.setStatus("PENDING");
        taxDraftRepository.save(draft);

        client.setStatus(ClientStatus.DRAFT_READY);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepository.save(client);

        if (assignment.getAssignedBy() != null) {
            notificationService.sendNotification(
                    assignment.getAssignedBy(),
                    "Tax Draft Ready",
                    employee.getFirstName() + " submitted a tax draft for client: " + client.getName(),
                    "DRAFT_READY","/clients"
            );
        }

        return mapToPrepDto(assignment);
    }
    @Transactional(readOnly = true)
    protected Long validateAndGetClientIdForPrep(Long assignmentId, Authentication authentication) {
        User employee = getLoggedInUser(authentication);
        ClientAssignment assignment = assignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
                .orElseThrow(() -> new RuntimeException("Assignment not found or not active"));
        return assignment.getClient().getId();
    }

    // --- HELPER 2: VERSION CALCULATION ---
    @Transactional(readOnly = true)
    protected int getNextDraftVersion(Long clientId) {
        return taxDraftRepository.findFirstByClientIdOrderByDraftVersionDesc(clientId)
                .map(draft -> draft.getDraftVersion() + 1)
                .orElse(1);
    }

    // --- HELPER 3: FILE I/O ---
    private Path saveDraftToDisk(Long clientId, int version, MultipartFile file) {
        try {
            Files.createDirectories(draftUploadDirectory);
            Path clientDir = draftUploadDirectory.resolve(String.valueOf(clientId));
            Files.createDirectories(clientDir);

            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".pdf";

            String storedName = "draft_v" + version + "_" + UUID.randomUUID() + extension;
            Path filePath = clientDir.resolve(storedName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save the draft file to the client folder", e);
        }
    }

//    // --- HELPER 4: DATABASE UPDATE ---
//    @Transactional
//    protected PrepClientResponseDto finalizeDraftSubmission(Long assignmentId, int version,
//                                                            MultipartFile file, Path filePath, String remarks,
//                                                            Authentication authentication) {
//
//        User employee = getLoggedInUser(authentication);
//        ClientAssignment assignment = assignmentRepository.findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
//                .orElseThrow(() -> new RuntimeException("Assignment not found or not active"));
//        Client client = assignment.getClient();
//
//        TaxDraft draft = new TaxDraft();
//        draft.setClient(client);
//        draft.setPrepEmployee(employee);
//        draft.setDraftVersion(version);
//        draft.setFileName(file.getOriginalFilename());
//        draft.setFilePath(filePath.toAbsolutePath().toString());
//        draft.setContentType(file.getContentType());
//        draft.setPrepRemarks(remarks);
//        draft.setStatus("PENDING");
//        taxDraftRepository.save(draft);
//
//        client.setStatus(ClientStatus.DRAFT_READY);
//        client.setUpdatedAt(LocalDateTime.now());
//        clientRepository.save(client);
//
//        if (assignment.getAssignedBy() != null) {
//            notificationService.sendNotification(
//                    assignment.getAssignedBy(),
//                    "Tax Draft Ready",
//                    employee.getFirstName() + " submitted a tax draft for client: " + client.getName(),
//                    "DRAFT_READY","/clients"
//            );
//        }
//
//        // mapToPrepDto runs safely inside the transaction, avoiding LazyInit crashes
//        return mapToPrepDto(assignment);
//    }

    @Transactional(readOnly = true)
    public List<TaxDraftResponseDto> getClientDrafts(Long clientId) {
        return taxDraftRepository.findByClientIdOrderByDraftVersionDesc(clientId)
                .stream()
                .map(draft -> TaxDraftResponseDto.builder()
                        .draftId(draft.getId())
                        .draftVersion(draft.getDraftVersion())
                        .fileName(draft.getFileName())
                        .prepRemarks(draft.getPrepRemarks())
                        .adminFeedback(draft.getAdminFeedback())
                        .status(draft.getStatus())
                        .uploadedAt(draft.getCreatedAt())
                        .prepEmployeeName(draft.getPrepEmployee().getFirstName())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource getDraftFile(Long draftId) {
        try {
            TaxDraft draft = taxDraftRepository.findById(draftId)
                    .orElseThrow(() -> new RuntimeException("Draft not found"));

            if (draft.getFilePath() == null || draft.getFilePath().isBlank()) {
                throw new RuntimeException("File path is missing for this draft.");
            }

            Path path = Paths.get(draft.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found on server or is unreadable");
            }
            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    @Transactional
    public AdminClientResponseDto reviewTaxDraft(Long draftId, boolean isApproved, String feedback,
                                                 Authentication authentication) {
        TaxDraft draft = taxDraftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        Client client = draft.getClient();

        if (isApproved) {
            draft.setStatus("APPROVED");
            client.setStatus(ClientStatus.DRAFT_APPROVED);
        } else {
            if (feedback == null || feedback.isBlank()) {
                throw new IllegalArgumentException("You must provide feedback when rejecting a draft.");
            }
            draft.setStatus("REJECTED");
            draft.setAdminFeedback(feedback);
            client.setStatus(ClientStatus.DRAFT_REJECTED);
        }

        taxDraftRepository.save(draft);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepository.save(client);

        return mapClient(client);
    }

    private AdminClientResponseDto mapClient(Client client) {
        ClientAssignment assignment = assignmentRepository.findByClientIdAndActiveTrue(client.getId()).orElse(null);

        return AdminClientResponseDto.builder()
                .clientId(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .status(client.getStatus())
                .currentStage(client.getCurrentStage())
                .nextFollowUpAt(client.getNextFollowUpAt())
                .assignedEmployeeId(assignment == null ? null : assignment.getEmployee().getId())
                .assignedEmployeeName(assignment == null ? null
                        : assignment.getEmployee().getFirstName() + " " + assignment.getEmployee().getLastName())
                .assignedAt(assignment == null ? null : assignment.getAssignedAt())
                .build();
    }

    @Transactional
    public PrepAssignmentResponseDto reassignPrepClient(Long assignmentId, ReassignClientRequestDto request,
                                                        Authentication authentication) {
        User admin = getLoggedInUser(authentication);
        ClientAssignment oldAssignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        User newEmployee = userRepository.findById(request.getNewEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (newEmployee.getDepartment() == null
                || !newEmployee.getDepartment().getName().equalsIgnoreCase("PREPARATION")) {
            throw new RuntimeException("You can only reassign to an employee in the Preparation team.");
        }

        oldAssignment.setActive(false);
        oldAssignment.setEndedAt(LocalDateTime.now());
        assignmentRepository.save(oldAssignment);

        ClientAssignment newAssignment = new ClientAssignment();
        newAssignment.setClient(oldAssignment.getClient());
        newAssignment.setEmployee(newEmployee);
        newAssignment.setAssignedBy(admin);
        newAssignment.setAssignedAt(LocalDateTime.now());
        newAssignment.setActive(true);
        newAssignment.setAssignmentReason(request.getReason());
        newAssignment.setCallInProgress(false);
        newAssignment = assignmentRepository.save(newAssignment);

        return mapToPrepAssignment(newAssignment);
    }

    @Transactional
    public List<PrepAssignmentResponseDto> bulkReassignPrepClients(BulkReassignClientRequestDto request,
                                                                   Authentication authentication) {
        User admin = getLoggedInUser(authentication);
        User newEmployee = userRepository.findById(request.getNewEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (newEmployee.getDepartment() == null
                || !newEmployee.getDepartment().getName().equalsIgnoreCase("PREPARATION")) {
            throw new RuntimeException("You can only reassign to an employee in the Preparation team.");
        }

        List<PrepAssignmentResponseDto> response = new java.util.ArrayList<>();

        for (Long assignmentId : request.getAssignmentIds()) {
            ClientAssignment oldAssignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

            if (!Boolean.TRUE.equals(oldAssignment.getActive())) {
                continue;
            }

            oldAssignment.setActive(false);
            oldAssignment.setEndedAt(LocalDateTime.now());
            assignmentRepository.save(oldAssignment);

            ClientAssignment newAssignment = new ClientAssignment();
            newAssignment.setClient(oldAssignment.getClient());
            newAssignment.setEmployee(newEmployee);
            newAssignment.setAssignedBy(admin);
            newAssignment.setAssignedAt(LocalDateTime.now());
            newAssignment.setActive(true);
            newAssignment.setAssignmentReason(request.getReason());
            newAssignment.setCallInProgress(false);
            newAssignment = assignmentRepository.save(newAssignment);

            response.add(mapToPrepAssignment(newAssignment));
        }
        return response;
    }

    private PrepAssignmentResponseDto mapToPrepAssignment(ClientAssignment assignment) {
        Client client = assignment.getClient();

        List<DocumentResponseDto> documents = clientDocumentRepository
                .findByClientIdOrderByUploadedAtDesc(client.getId())
                .stream()
                .map(doc -> DocumentResponseDto.builder()
                        .documentId(doc.getId())
                        .documentType(doc.getDocumentType())
                        .fileName(doc.getFileName())
                        .contentType(doc.getContentType())
                        .fileSize(doc.getFileSize())
                        .status(doc.getStatus())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .toList();

        return PrepAssignmentResponseDto.builder()
                .assignmentId(assignment.getId())
                .clientId(client.getId())
                .clientName(client.getName())
                .prepEmployeeId(assignment.getEmployee().getId())
                .prepEmployeeName(
                        assignment.getEmployee().getFirstName() + " " + assignment.getEmployee().getLastName())
                .status(client.getStatus().name())
                .documents(documents)
                .build();
    }

    @Transactional
    public AdminCommentResponseDto addComment(Long assignmentId, AddCommentRequestDto dto, Authentication authentication) {
        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = assignmentRepository
                .findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
                .orElseThrow(() -> new RuntimeException("Assignment not found or not active"));

        Client client = assignment.getClient();

        ClientComment comment = new ClientComment();
        comment.setClient(client);
        comment.setEmployee(employee);
        comment.setAssignment(assignment);
        comment.setComment(dto.getComment());

        comment.setCommentType("PREP_QUERY");
        comment.setDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        ClientComment savedComment = commentRepository.save(comment);

        AdminCommentResponseDto response = new AdminCommentResponseDto();
        response.setId(savedComment.getId());
        response.setComment(savedComment.getComment());
        response.setCommentType(savedComment.getCommentType());
        response.setCreatedAt(savedComment.getCreatedAt());
        response.setUpdatedAt(savedComment.getUpdatedAt());
        response.setEmployeeId(employee.getId());
        response.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        response.setClientId(client.getId());

        return response;
    }

    @Transactional
    public PrepClientResponseDto updateStatus(Long assignmentId, UpdateClientStatusDto request, Authentication authentication) {
        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = assignmentRepository
                .findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
                .orElseThrow(() -> new RuntimeException("Assignment not found or not active"));

        Client client = assignment.getClient();
        client.setStatus(request.getStatus());
        client.setUpdatedAt(LocalDateTime.now());

        clientRepository.save(client);

        return mapToPrepDto(assignment);
    }

    @Transactional(readOnly = true)
    public ClientDocument getClientDocument(
            Long assignmentId,
            Long documentId,
            Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        ClientAssignment assignment = assignmentRepository
                .findByIdAndEmployeeAndActiveTrue(assignmentId, employee)
                .orElseThrow(() ->
                        new RuntimeException("Assignment not found or not active"));

        ClientDocument document = clientDocumentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        if (!document.getClient().getId().equals(assignment.getClient().getId())) {
            throw new RuntimeException(
                    "Document does not belong to your assigned client");
        }

        return document;
    }
}