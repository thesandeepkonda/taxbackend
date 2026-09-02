package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;


    // =========================================================
    // DOC EMPLOYEE
    // =========================================================

    /*
     * Create document request
     */
    @PostMapping("/request")
    public ResponseEntity<DocumentRequestResponseDto> createRequest(
            @RequestBody CreateDocumentRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                documentService.createRequest(
                        dto,
                        authentication
                )
        );
    }


    /*
     * DOC employee sees his requests.
     *
     * IMPORTANT:
     * This returns document details/status only.
     * There is NO DOC employee file-view endpoint.
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<DocumentRequestResponseDto>>
    getMyRequests(
            Authentication authentication) {

        return ResponseEntity.ok(
                documentService.getMyRequests(
                        authentication
                )
        );
    }


    /*
     * DOC employee gets one request.
     */
    @GetMapping("/my-requests/{requestId}")
    public ResponseEntity<DocumentRequestResponseDto>
    getMyRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        return ResponseEntity.ok(
                documentService.getMyRequest(
                        requestId,
                        authentication
                )
        );
    }


    // =========================================================
    // CLIENT PUBLIC LINK
    // =========================================================

    /*
     * Client opens:
     *
     * GET
     * /api/documents/public/{token}
     */
    @GetMapping("/public/{token}")
    public ResponseEntity<?> getPublicRequest(
            @PathVariable String token) {

        try {
            System.out.println("===== PUBLIC DOCUMENT REQUEST =====");
            System.out.println("Token: " + token);

            DocumentRequestResponseDto response =
                    documentService.getPublicRequest(token);

            System.out.println("Request found: "
                    + response.getRequestId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() == null
                                    ? "Unknown error"
                                    : e.getMessage()
                    ));
        }
    }


    /*
     * Client uploads document.
     *
     * POST
     * /api/documents/public/{token}/upload/{documentId}
     *
     * Content-Type:
     * multipart/form-data
     *
     * file = actual file
     */
    @PostMapping(
            value = "/public/{token}/upload/{documentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponseDto>
    uploadDocument(
            @PathVariable String token,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                documentService.uploadDocument(
                        token,
                        documentId,
                        file
                )
        );
    }


    /*
     * Client clicks SUBMIT.
     */
    @PostMapping("/public/{token}/submit")
    public ResponseEntity<DocumentRequestResponseDto>
    submitDocuments(
            @PathVariable String token) {

        return ResponseEntity.ok(
                documentService.submitDocuments(
                        token
                )
        );
    }


    /*
     * CLIENT can view/download his uploaded file.
     *
     * The token is required.
     *
     * Therefore another client cannot simply change
     * documentId and access someone else's document.
     */
    @GetMapping("/public/{token}/view/{documentId}")
    public ResponseEntity<Resource>
    clientViewDocument(
            @PathVariable String token,
            @PathVariable Long documentId) {

        Resource resource =
                documentService.getClientDocument(
                        token,
                        documentId
                );

        return ResponseEntity.ok()
                .contentType(
                        getMediaType(resource)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                resource.getFilename() +
                                "\""
                )
                .body(resource);
    }


    // =========================================================
    // ADMIN
    // =========================================================

    /*
     * ONLY ADMIN should have access to this endpoint.
     *
     * Security configuration must protect:
     *
     * /api/documents/admin/**
     */
    @GetMapping("/admin/view/{documentId}")
    public ResponseEntity<Resource>
    adminViewDocument(
            @PathVariable Long documentId) {

        Resource resource =
                documentService.getDocumentForAdmin(
                        documentId
                );

        return ResponseEntity.ok()
                .contentType(
                        getMediaType(resource)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                resource.getFilename() +
                                "\""
                )
                .body(resource);
    }


    // =========================================================
    // CONTENT TYPE
    // =========================================================

    private MediaType getMediaType(
            Resource resource) {

        try {

            String filename =
                    resource.getFilename();

            if (filename == null) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }

            String lower =
                    filename.toLowerCase();

            if (lower.endsWith(".pdf")) {
                return MediaType.APPLICATION_PDF;
            }

            if (lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg")) {

                return MediaType.IMAGE_JPEG;
            }

            if (lower.endsWith(".png")) {
                return MediaType.IMAGE_PNG;
            }

            if (lower.endsWith(".txt")) {
                return MediaType.TEXT_PLAIN;
            }

            return MediaType.APPLICATION_OCTET_STREAM;

        } catch (Exception e) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
