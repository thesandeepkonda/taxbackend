package com.crm.matrix.service;

import com.crm.matrix.dto.CreateDocumentRequestDto;
import com.crm.matrix.dto.DocumentRequestResponseDto;
import com.crm.matrix.dto.DocumentResponseDto;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.ClientDocument;
import com.crm.matrix.entity.DocumentRequest;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.DocumentStatus;
import com.crm.matrix.repository.ClientDocumentRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.DocumentRequestRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor

public class DocumentService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DocumentRequestRepository requestRepository;
    private final ClientDocumentRepository documentRepository;
    private final NotificationService notificationService;

    private final Path uploadDirectory = Paths.get("uploads/documents");


    // =========================================================
    // GET LOGGED-IN EMPLOYEE
    // =========================================================

    private User getLoggedInUser(Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        return userRepository.findByEmployeeCode(authentication.getName()).orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }


    // =========================================================
    // 1. CREATE DOCUMENT REQUEST
    // =========================================================

    @Transactional
    public DocumentRequestResponseDto createRequest(CreateDocumentRequestDto dto, Authentication authentication) {

        User employee = getLoggedInUser(authentication);

        Client client = clientRepository.findById(dto.getClientId()).orElseThrow(() -> new RuntimeException("Client not found"));


        DocumentRequest request = new DocumentRequest();

        request.setClient(client);
        request.setEmployee(employee);

        request.setShareToken(UUID.randomUUID().toString().replace("-", ""));

        request.setExpiresAt(dto.getExpiresAt());

        request.setActive(true);
        request.setSubmitted(false);

        request = requestRepository.save(request);


        // =====================================================
        // CREATE DOCUMENT SLOTS
        // =====================================================

        if (dto.getDocumentTypes() != null) {

            for (String type : dto.getDocumentTypes()) {

                if (type == null || type.isBlank()) {
                    continue;
                }

                ClientDocument document = ClientDocument.builder().request(request).client(client).documentType(type.trim()).fileName(null).filePath(null).contentType(null).fileSize(null).status(DocumentStatus.PENDING).build();

                documentRepository.save(document);
            }
        }

        return mapRequest(request);
    }


    // =========================================================
    // 2. CLIENT OPENS PUBLIC DOCUMENT LINK
    // =========================================================

    @Transactional(readOnly = true)
    public DocumentRequestResponseDto getPublicRequest(String token) {

        DocumentRequest request = getValidRequest(token);

        return mapRequest(request);
    }


    // =========================================================
    // 3. CLIENT UPLOADS DOCUMENT
    // =========================================================
    public DocumentResponseDto uploadDocument(String token, Long documentId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // STEP 1: Transactional Read & Validation (Fast)
        Long clientId = validateAndGetClientId(token, documentId);

        // STEP 2: Non-Transactional Disk I/O (Slow, but DB connection is safe in the pool!)
        Path savedFilePath = saveFileToDisk(clientId, file);

        // STEP 3: Transactional Write (Fast)
        return finalizeDocumentRecord(documentId, file, savedFilePath);
    }

    @Transactional(readOnly = true)
    protected Long validateAndGetClientId(String token, Long documentId) {
        DocumentRequest request = getValidRequest(token);
        ClientDocument document = documentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getClient() == null || request.getClient() == null || !document.getClient().getId().equals(request.getClient().getId())) {
            throw new RuntimeException("You are not allowed to upload this document");
        }

        // This safely triggers any lazy loading required and returns the ID we need for the folder path
        return request.getClient().getId();
    }

    // --- HELPER 2: FILE I/O ---
    private Path saveFileToDisk(Long clientId, MultipartFile file) {
        try {
            Files.createDirectories(uploadDirectory);
            Path clientDirectory = uploadDirectory.resolve(String.valueOf(clientId));
            Files.createDirectories(clientDirectory);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String storedName = UUID.randomUUID() + extension;
            Path filePath = clientDirectory.resolve(storedName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Unable to upload document", e);
        }
    }

    // --- HELPER 3: DATABASE UPDATE ---
    @Transactional
    protected DocumentResponseDto finalizeDocumentRecord(Long documentId, MultipartFile file, Path filePath) {
        ClientDocument document = documentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath.toAbsolutePath().toString());
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.SUBMITTED);
        document.setUploadedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentRepository.save(document);
        return mapDocument(document);
    }

    // =========================================================
    // 4. CLIENT SUBMITS ALL DOCUMENTS
    // =========================================================

    @Transactional
    public DocumentRequestResponseDto submitDocuments(String token) {

        DocumentRequest request = getValidRequest(token);


        List<ClientDocument> documents = documentRepository.findByRequestOrderByUpdatedAtDesc(request);


        if (documents.isEmpty()) {

            throw new RuntimeException("No documents were requested");
        }


        boolean allSubmitted = documents.stream().allMatch(d -> d.getStatus() == DocumentStatus.SUBMITTED || d.getStatus() == DocumentStatus.VERIFIED);


        if (!allSubmitted) {

            throw new RuntimeException("Please upload all required documents");
        }


        request.setSubmitted(true);

        request.setSubmittedAt(LocalDateTime.now());


        requestRepository.save(request);

        notificationService.sendNotification(request.getEmployee(), "Documents Submitted", "Client " + request.getClient().getName() + " has submitted all requested documents.", "DOCUMENT_SUBMITTED","/clients");

        return mapRequest(request);
    }


    // =========================================================
    // 5. DOC EMPLOYEE GETS HIS REQUESTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocumentRequestResponseDto> getMyRequests(Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        return requestRepository.findByEmployeeAndActiveTrueOrderByCreatedAtDesc(employee).stream().map(this::mapRequest).toList();
    }


    // =========================================================
    // 6. DOC EMPLOYEE GETS ONE REQUEST
    // =========================================================

    @Transactional(readOnly = true)
    public DocumentRequestResponseDto getMyRequest(Long requestId, Authentication authentication) {

        User employee = getLoggedInUser(authentication);


        DocumentRequest request = requestRepository.findByIdAndEmployee(requestId, employee).orElseThrow(() -> new RuntimeException("Document request not found"));


        return mapRequest(request);
    }


    // =========================================================
    // 7. ADMIN VIEW DOCUMENT
    // =========================================================

    @Transactional(readOnly = true)
    public Resource getDocumentForAdmin(Long documentId) {

        ClientDocument document = documentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));


        if (document.getStatus() == DocumentStatus.PENDING) {

            throw new RuntimeException("Document has not been uploaded");
        }


        return getFile(document);
    }


    // =========================================================
    // 8. CLIENT VIEW OWN DOCUMENT
    // =========================================================

    @Transactional(readOnly = true)
    public Resource getClientDocument(String token, Long documentId) {

        DocumentRequest request = getValidRequest(token);


        ClientDocument document = documentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));


        // =====================================================
        // SECURITY CHECK
        // =====================================================

        if (document.getClient() == null || !document.getClient().getId().equals(request.getClient().getId())) {

            throw new RuntimeException("You are not allowed to view this document");
        }


        if (document.getStatus() == DocumentStatus.PENDING) {

            throw new RuntimeException("Document has not been uploaded");
        }


        return getFile(document);
    }


    // =========================================================
    // 9. GET VALID REQUEST
    // =========================================================

    private DocumentRequest getValidRequest(String token) {

        if (token == null || token.isBlank()) {

            throw new RuntimeException("Document token is required");
        }


        DocumentRequest request = requestRepository.findByShareTokenAndActiveTrue(token).orElseThrow(() -> new RuntimeException("Invalid document link"));


        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Document link has expired");
        }


        return request;
    }


    // =========================================================
    // 10. GET FILE
    // =========================================================

    private Resource getFile(ClientDocument document) {

        try {

            if (document.getFilePath() == null || document.getFilePath().isBlank()) {

                throw new RuntimeException("File path not available");
            }


            Path path = Paths.get(document.getFilePath()).toAbsolutePath().normalize();


            Resource resource = new UrlResource(path.toUri());


            if (!resource.exists() || !resource.isReadable()) {

                throw new RuntimeException("File not found");
            }


            return resource;


        } catch (MalformedURLException e) {

            throw new RuntimeException("Unable to read document", e);
        }
    }


    // =========================================================
    // 11. REQUEST MAPPER
    // =========================================================

    private DocumentRequestResponseDto mapRequest(DocumentRequest request) {

        List<ClientDocument> documents = documentRepository.findByRequestOrderByUpdatedAtDesc(request);


        return DocumentRequestResponseDto.builder()

                .requestId(request.getId())

                .clientId(request.getClient().getId())

                .clientName(request.getClient().getName())

                .shareToken(request.getShareToken())

                .shareUrl("/document-upload/" + request.getShareToken())

                .active(request.getActive())

                .submitted(request.getSubmitted())

                .expiresAt(request.getExpiresAt())

                .documents(documents.stream().map(this::mapDocument).toList())

                .build();
    }


    // =========================================================
    // 12. DOCUMENT MAPPER
    // =========================================================

    private DocumentResponseDto mapDocument(ClientDocument document) {

        return DocumentResponseDto.builder()

                .documentId(document.getId())

                .documentType(document.getDocumentType())

                .fileName(document.getFileName())

                .contentType(document.getContentType())

                .fileSize(document.getFileSize())

                .status(document.getStatus())

                .uploadedAt(document.getUploadedAt())

                .build();
    }


}