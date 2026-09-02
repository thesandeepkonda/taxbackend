
package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DocumentRequestRepository requestRepository;
    private final ClientDocumentRepository documentRepository;

    /*
     * Files are stored outside the public/static folder.
     *
     * DO NOT put these files inside:
     * src/main/resources/static
     * src/main/resources/public
     *
     * Otherwise anybody could access them directly.
     */
    private final Path uploadDirectory =
            Paths.get("uploads/documents");


    // =========================================================
    // GET LOGGED-IN EMPLOYEE
    // =========================================================

    private User getLoggedInUser(
            Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        return userRepository
                .findByEmployeeCode(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"
                        ));
    }


    // =========================================================
    // 1. DOC EMPLOYEE CREATES DOCUMENT REQUEST
    // =========================================================

    public DocumentRequestResponseDto createRequest(
            CreateDocumentRequestDto dto,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        Client client =
                clientRepository
                        .findById(dto.getClientId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client not found"
                                ));


        DocumentRequest request =
                new DocumentRequest();

        request.setClient(client);

        request.setEmployee(employee);

        /*
         * Random token.
         *
         * Example:
         * https://yourdomain.com/document-upload/8f7a...
         */
        request.setShareToken(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
        );

        request.setExpiresAt(
                dto.getExpiresAt()
        );

        request.setActive(true);

        request.setSubmitted(false);


        request =
                requestRepository.save(request);


        // =====================================================
        // CREATE REQUIRED DOCUMENT SLOTS
        // =====================================================

        if (dto.getDocumentTypes() != null) {

            for (String type :
                    dto.getDocumentTypes()) {

                if (type == null ||
                        type.isBlank()) {
                    continue;
                }

                ClientDocument document =
                        new ClientDocument();

                document.setRequest(request);

                document.setClient(client);

                document.setDocumentType(
                        type.trim()
                );

                document.setDocumentName(
                        type.trim()
                );

                document.setUploaded(false);

                document.setVerified(false);

                /*
                 * Do not use null if your DB/DTO does not
                 * handle it properly.
                 */
                document.setFileName(null);

                document.setFilePath(null);

                documentRepository.save(document);
            }
        }


        return mapRequest(request);
    }


    // =========================================================
    // 2. CLIENT OPENS PUBLIC DOCUMENT LINK
    // =========================================================

    @Transactional(readOnly = true)
    public DocumentRequestResponseDto getPublicRequest(
            String token) {

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Document link token is required");
        }

        DocumentRequest request =
                requestRepository
                        .findByShareTokenAndActiveTrue(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid document link"
                                ));

        if (request.getExpiresAt() != null &&
                request.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Document link has expired"
            );
        }

        return mapPublicRequest(request);
    }

    private DocumentRequestResponseDto mapPublicRequest(
            DocumentRequest request) {

        List<ClientDocument> documents =
                documentRepository
                        .findByRequestOrderByCreatedAtDesc(
                                request
                        );

        return DocumentRequestResponseDto
                .builder()
                .requestId(request.getId())
                .clientId(request.getClient().getId())
                .clientName(request.getClient().getName())
                .shareToken(request.getShareToken())
                .shareUrl(
                        "/api/documents/public/"
                                + request.getShareToken()
                )
                .active(request.getActive())
                .submitted(request.getSubmitted())
                .expiresAt(request.getExpiresAt())
                .documents(
                        documents.stream()
                                .map(this::mapDocument)
                                .toList()
                )
                .build();
    }


    // =========================================================
    // 3. CLIENT UPLOADS DOCUMENT
    // =========================================================

    public DocumentResponseDto uploadDocument(
            String token,
            Long documentId,
            MultipartFile file) {

        if (file == null ||
                file.isEmpty()) {

            throw new RuntimeException(
                    "File is empty"
            );
        }


        DocumentRequest request =
                getValidRequest(token);


        ClientDocument document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found"
                                ));


        // =====================================================
        // SECURITY CHECK
        // =====================================================

        /*
         * The document MUST belong to the request represented
         * by the share token.
         */
        if (document.getRequest() == null ||
                !document.getRequest()
                        .getId()
                        .equals(request.getId())) {

            throw new RuntimeException(
                    "You are not allowed to upload this document"
            );
        }


        try {

            Files.createDirectories(
                    uploadDirectory
            );


            // =================================================
            // CLIENT DIRECTORY
            // =================================================

            Path clientDirectory =
                    uploadDirectory
                            .resolve(
                                    String.valueOf(
                                            request
                                                    .getClient()
                                                    .getId()
                                    )
                            );


            Files.createDirectories(
                    clientDirectory
            );


            // =================================================
            // GENERATE SAFE FILE NAME
            // =================================================

            String originalName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalName != null &&
                    originalName.contains(".")) {

                extension =
                        originalName.substring(
                                originalName.lastIndexOf(".")
                        );
            }


            String storedName =
                    UUID.randomUUID()
                            + extension;


            Path filePath =
                    clientDirectory
                            .resolve(storedName);


            // =================================================
            // SAVE FILE
            // =================================================

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            // =================================================
            // UPDATE DATABASE
            // =================================================

            document.setFileName(
                    originalName
            );

            document.setFilePath(
                    filePath.toAbsolutePath()
                            .toString()
            );

            document.setContentType(
                    file.getContentType()
            );

            document.setFileSize(
                    file.getSize()
            );

            document.setUploaded(true);

            document.setUploadedAt(
                    LocalDateTime.now()
            );


            documentRepository.save(document);


            return mapDocument(document);


        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to upload document",
                    e
            );
        }
    }


    // =========================================================
    // 4. CLIENT SUBMITS ALL DOCUMENTS
    // =========================================================

    public DocumentRequestResponseDto submitDocuments(
            String token) {

        DocumentRequest request =
                getValidRequest(token);


        List<ClientDocument> documents =
                documentRepository
                        .findByRequestOrderByCreatedAtDesc(
                                request
                        );


        if (documents.isEmpty()) {

            throw new RuntimeException(
                    "No documents were requested"
            );
        }


        boolean allUploaded =
                documents.stream()
                        .allMatch(
                                d -> Boolean.TRUE.equals(
                                        d.getUploaded()
                                )
                        );


        if (!allUploaded) {

            throw new RuntimeException(
                    "Please upload all required documents"
            );
        }


        request.setSubmitted(true);

        request.setSubmittedAt(
                LocalDateTime.now()
        );


        requestRepository.save(request);


        return mapRequest(request);
    }


    // =========================================================
    // 5. DOC EMPLOYEE GETS HIS REQUESTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DocumentRequestResponseDto> getMyRequests(
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);


        return requestRepository
                .findByEmployeeAndActiveTrueOrderByCreatedAtDesc(
                        employee
                )
                .stream()
                .map(this::mapRequest)
                .toList();
    }


    // =========================================================
    // 6. DOC EMPLOYEE GETS ONE REQUEST
    // =========================================================

    @Transactional(readOnly = true)
    public DocumentRequestResponseDto getMyRequest(
            Long requestId,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);


        DocumentRequest request =
                requestRepository
                        .findByIdAndEmployee(
                                requestId,
                                employee
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document request not found"
                                ));


        return mapRequest(request);
    }


    // =========================================================
    // 7. ADMIN VIEW DOCUMENT
    // =========================================================

    @Transactional(readOnly = true)
    public Resource getDocumentForAdmin(
            Long documentId) {

        ClientDocument document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found"
                                ));


        if (!Boolean.TRUE.equals(
                document.getUploaded())) {

            throw new RuntimeException(
                    "Document has not been uploaded"
            );
        }


        return getFile(document);
    }


    // =========================================================
    // 8. CLIENT VIEW OWN DOCUMENT
    // =========================================================

    /*
     * IMPORTANT:
     *
     * Client does NOT need login.
     *
     * The share token acts as the authorization.
     *
     * The client can only access a document if:
     *
     * document.request.id == token.request.id
     */
    @Transactional(readOnly = true)
    public Resource getClientDocument(
            String token,
            Long documentId) {

        DocumentRequest request =
                getValidRequest(token);


        ClientDocument document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found"
                                ));


        // =====================================================
        // VERY IMPORTANT SECURITY CHECK
        // =====================================================

        if (document.getRequest() == null ||
                !document.getRequest()
                        .getId()
                        .equals(request.getId())) {

            throw new RuntimeException(
                    "You are not allowed to view this document"
            );
        }


        if (!Boolean.TRUE.equals(
                document.getUploaded())) {

            throw new RuntimeException(
                    "Document has not been uploaded"
            );
        }


        return getFile(document);
    }


    // =========================================================
    // 9. GET VALID REQUEST FROM TOKEN
    // =========================================================

    private DocumentRequest getValidRequest(
            String token) {

        if (token == null ||
                token.isBlank()) {

            throw new RuntimeException(
                    "Document token is required"
            );
        }


        DocumentRequest request =
                requestRepository
                        .findByShareTokenAndActiveTrue(
                                token
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid document link"
                                ));


        if (request.getExpiresAt() != null &&
                request.getExpiresAt()
                        .isBefore(
                                LocalDateTime.now()
                        )) {

            throw new RuntimeException(
                    "Document link has expired"
            );
        }


        return request;
    }


    // =========================================================
    // 10. GET FILE
    // =========================================================

    private Resource getFile(
            ClientDocument document) {

        try {

            if (document.getFilePath() == null ||
                    document.getFilePath().isBlank()) {

                throw new RuntimeException(
                        "File path not available"
                );
            }


            Path path =
                    Paths.get(
                                    document.getFilePath()
                            )
                            .toAbsolutePath()
                            .normalize();


            Resource resource =
                    new UrlResource(
                            path.toUri()
                    );


            if (!resource.exists() ||
                    !resource.isReadable()) {

                throw new RuntimeException(
                        "File not found"
                );
            }


            return resource;


        } catch (MalformedURLException e) {

            throw new RuntimeException(
                    "Unable to read document",
                    e
            );
        }
    }


    // =========================================================
    // 11. REQUEST MAPPER
    // =========================================================

    private DocumentRequestResponseDto mapRequest(
            DocumentRequest request) {

        List<ClientDocument> documents =
                documentRepository
                        .findByRequestOrderByCreatedAtDesc(
                                request
                        );


        return DocumentRequestResponseDto
                .builder()

                .requestId(
                        request.getId()
                )

                .clientId(
                        request.getClient()
                                .getId()
                )

                .clientName(
                        request.getClient()
                                .getName()
                )

                .shareToken(
                        request.getShareToken()
                )

                /*
                 * Frontend can append this token to your
                 * frontend URL.
                 *
                 * Example:
                 *
                 * http://localhost:3000/document-upload/TOKEN
                 */
                .shareUrl(
                        "/document-upload/"
                                + request.getShareToken()
                )

                .active(
                        request.getActive()
                )

                .submitted(
                        request.getSubmitted()
                )

                .expiresAt(
                        request.getExpiresAt()
                )

                .documents(
                        documents.stream()
                                .map(this::mapDocument)
                                .toList()
                )

                .build();
    }


    // =========================================================
    // 12. DOCUMENT MAPPER
    // =========================================================

    private DocumentResponseDto mapDocument(
            ClientDocument document) {

        /*
         * NEVER return:
         *
         * filePath
         *
         * This prevents the DOC employee/frontend from
         * knowing where the physical file is stored.
         */

        return DocumentResponseDto
                .builder()

                .documentId(
                        document.getId()
                )

                .documentType(
                        document.getDocumentType()
                )

                .documentName(
                        document.getDocumentName()
                )

                .fileName(
                        document.getFileName()
                )

                .contentType(
                        document.getContentType()
                )

                .fileSize(
                        document.getFileSize()
                )

                .uploaded(
                        document.getUploaded()
                )

                .verified(
                        document.getVerified()
                )

                .remarks(
                        document.getRemarks()
                )

                .uploadedAt(
                        document.getUploadedAt()
                )

                .build();
    }
}

