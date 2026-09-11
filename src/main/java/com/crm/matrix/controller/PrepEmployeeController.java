package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.ClientDocument;
import com.crm.matrix.service.AdminCRMService;
import com.crm.matrix.service.PrepEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/prep")
@RequiredArgsConstructor
public class PrepEmployeeController {

    private final PrepEmployeeService prepEmployeeService;
    private final AdminCRMService adminCRMService;



    @PostMapping("/clients/bulk-assign-prep")
    public ResponseEntity<List<AdminClientResponseDto>> bulkAssignToPreparation(
            @Valid @RequestBody BulkAssignPrepRequestDto request,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.bulkAssignToPreparation(request, authentication)
        );
    }

    @PostMapping("/assignments/prep/{assignmentId}/reassign")
    public ResponseEntity<PrepAssignmentResponseDto> reassignPrepClient(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ReassignClientRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(prepEmployeeService.reassignPrepClient(assignmentId, request, authentication));
    }

    @PostMapping("/assignments/prep/bulk-reassign")
    public ResponseEntity<List<PrepAssignmentResponseDto>> bulkReassignPrepClients(
            @Valid @RequestBody BulkReassignClientRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(prepEmployeeService.bulkReassignPrepClients(request, authentication));
    }

    @GetMapping("/clients")
    public ResponseEntity<Page<PrepClientResponseDto>> getQueue(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                prepEmployeeService.getMyPrepClients(authentication, pageable)
        );
    }

    @GetMapping(value = "/clients/{assignmentId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadSourceDocument(
            @PathVariable Long assignmentId,
            @PathVariable Long documentId,
            Authentication authentication) {

        ClientDocument document = prepEmployeeService.getClientDocument(assignmentId, documentId, authentication);
        Resource file = prepEmployeeService.getClientSourceDocument(document);
        MediaType mediaType = getMediaType(document);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(file);
    }

    @PostMapping(value = "/clients/{assignmentId}/submit-draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrepClientResponseDto> submitDraft(
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {

        PrepClientResponseDto response = prepEmployeeService.submitDraft(assignmentId, file, remarks, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clients/{clientId}/drafts")
    public ResponseEntity<List<TaxDraftResponseDto>> getClientDrafts(@PathVariable Long clientId) {
        return ResponseEntity.ok(prepEmployeeService.getClientDrafts(clientId));
    }

    @GetMapping("/drafts/{draftId}/view")
    public ResponseEntity<Resource> viewDraftFile(@PathVariable Long draftId) {
        Resource resource = prepEmployeeService.getDraftFile(draftId);
        return ResponseEntity.ok()
                .contentType(getMediaTypeForDraft(resource))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PutMapping("/drafts/{draftId}/review")
    public ResponseEntity<AdminClientResponseDto> reviewDraft(
            @PathVariable Long draftId,
            @RequestParam boolean isApproved,
            @RequestParam(required = false) String feedback,
            Authentication authentication) {

        return ResponseEntity.ok(prepEmployeeService.reviewTaxDraft(draftId, isApproved, feedback, authentication));
    }

    @GetMapping("/clients/{assignmentId}/documents/{documentId}/view")
    public ResponseEntity<Resource> viewSourceDocument(
            @PathVariable Long assignmentId,
            @PathVariable Long documentId,
            Authentication authentication) {

        ClientDocument document = prepEmployeeService.getClientDocument(
                assignmentId,
                documentId,
                authentication
        );

        Resource resource = prepEmployeeService.getClientSourceDocument(
                document
        );

        MediaType mediaType = getMediaType(document);

        try {
            // Explicitly set content length so Postman/Browser knows it's a renderable stream
            long contentLength = resource.contentLength();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + document.getFileName() + "\""
                    )
                    .body(resource);
        } catch (Exception e) {
            // Fallback if resource length cannot be read directly
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + document.getFileName() + "\""
                    )
                    .body(resource);
        }
    }

    private MediaType getMediaType(ClientDocument document) {
        // 1. Check filename extension first to prioritize exact file preview rendering over corrupted DB metadata
        String fileName = document.getFileName();
        MediaType resolved = resolveMediaTypeByFilename(fileName);
        if (!resolved.equals(MediaType.APPLICATION_OCTET_STREAM)) {
            return resolved;
        }

        // 2. Fallback to database content type if extension check is inconclusive
        String contentType = document.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private MediaType getMediaTypeForDraft(Resource resource) {
        return resolveMediaTypeByFilename(resource.getFilename());
    }

    private MediaType resolveMediaTypeByFilename(String fileName) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
            if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
            if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
            if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;
            if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
            if (lower.endsWith(".csv")) return MediaType.parseMediaType("text/csv");
            if (lower.endsWith(".xml")) return MediaType.APPLICATION_XML;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @GetMapping("/clients/ready-for-review")
    public ResponseEntity<Page<PrepClientResponseDto>> getReadyForReviewQueue(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                prepEmployeeService.getReadyForReviewClients(authentication, pageable)
        );
    }

    @PostMapping("/assignments/{assignmentId}/comments")
    public ResponseEntity<AdminCommentResponseDto> addComment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AddCommentRequestDto request,
            Authentication authentication) {

        AdminCommentResponseDto response = prepEmployeeService.addComment(assignmentId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/assignments/{assignmentId}/status")
    public ResponseEntity<PrepClientResponseDto> updateStatus(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateClientStatusDto request,
            Authentication authentication) {

        PrepClientResponseDto response = prepEmployeeService.updateStatus(assignmentId, request, authentication);
        return ResponseEntity.ok(response);
    }
}