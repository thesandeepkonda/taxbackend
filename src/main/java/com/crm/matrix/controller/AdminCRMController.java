package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.enums.AssignmentPeriod;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.service.AdminCRMService;
import com.crm.matrix.service.TaxOrganizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminCRMController {

    private final AdminCRMService adminCRMService;
    private final TaxOrganizerService taxOrganizerService;


    @PostMapping(value = "/client-imports/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientImportResponseDto> uploadExcel(@RequestParam("file") MultipartFile file, Authentication authentication) {

        System.out.println("========== EXCEL UPLOAD ==========");
        System.out.println("File: " + file.getOriginalFilename());
        System.out.println("Size: " + file.getSize());
        System.out.println("User: " + authentication.getName());

        return ResponseEntity.ok(adminCRMService.uploadExcel(file, authentication));
    }

    @GetMapping("/clients")
    public ResponseEntity<Page<AdminClientResponseDto>> getClients(
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));

        return ResponseEntity.ok(adminCRMService.getClients(stage, pageable));
    }


    @GetMapping("/clients/search")
    public ResponseEntity<Page<AdminClientSearchResponseDto>> searchClients(

            @RequestParam(required = false) Long clientId,

            @RequestParam(required = false) String name,

            @RequestParam(required = false) AssignmentPeriod period,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));

        return ResponseEntity.ok(adminCRMService.searchClients(clientId, name, period, fromDate, toDate, pageable));
    }


    @GetMapping("/clients/{clientId}")
    public ResponseEntity<AdminClientResponseDto> getClient(@PathVariable Long clientId) {

        return ResponseEntity.ok(adminCRMService.getClient(clientId));
    }


    @PostMapping("/assignments/bulk")
    public ResponseEntity<List<AssignmentResponseDto>> bulkAssign(@Valid @RequestBody BulkAssignClientRequestDto request, Authentication authentication) {

        return ResponseEntity.ok(adminCRMService.bulkAssign(request, authentication));
    }


    @PostMapping("/assignments/{assignmentId}/reassign")
    public ResponseEntity<AssignmentResponseDto> reassign(@PathVariable Long assignmentId,

                                                          @Valid @RequestBody ReassignClientRequestDto request,

                                                          Authentication authentication) {

        return ResponseEntity.ok(adminCRMService.reassign(assignmentId, request, authentication));
    }


    @GetMapping("/clients/follow-ups")
    public ResponseEntity<List<AssignmentResponseDto>> getFollowUps() {

        return ResponseEntity.ok(adminCRMService.getFollowUps());
    }


    @GetMapping("/clients/not-lifted")
    public ResponseEntity<List<AssignmentResponseDto>> getNotLifted() {

        return ResponseEntity.ok(adminCRMService.getNotLifted());
    }


    @GetMapping("/calls")
    public ResponseEntity<Page<AdminCallResponseDto>> getCalls(Pageable pageable) {

        return ResponseEntity.ok(adminCRMService.getCalls(pageable));
    }


    @GetMapping("/clients/{clientId}/calls")
    public ResponseEntity<Page<AdminCallResponseDto>> getClientCalls(@PathVariable Long clientId, Pageable pageable) {

        return ResponseEntity.ok(adminCRMService.getClientCalls(clientId, pageable));
    }


    @GetMapping("/calls/{callId}/recording")
    public ResponseEntity<AdminCallResponseDto> getRecording(@PathVariable Long callId) {

        return ResponseEntity.ok(adminCRMService.getRecording(callId));
    }


    @GetMapping("/clients/{clientId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long clientId) {

        return ResponseEntity.ok(adminCRMService.getClientComments(clientId));
    }


    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {

        adminCRMService.deleteComment(commentId, authentication);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/reports/employees/{employeeId}")
    public ResponseEntity<EmployeeCallReportDto> employeeReport(

            @PathVariable Long employeeId,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(adminCRMService.getEmployeeReport(employeeId, from, to));
    }

    @PostMapping("/assignments/bulk-reassign")
    public ResponseEntity<List<AssignmentResponseDto>> bulkReassign(@Valid @RequestBody BulkReassignClientRequestDto request, Authentication authentication) {

        return ResponseEntity.ok(adminCRMService.bulkReassign(request, authentication));
    }

    @PutMapping("/clients/{clientId}/status")
    public ResponseEntity<AdminClientResponseDto> updateClientStatus(

            @PathVariable Long clientId,

            @Valid @RequestBody UpdateClientStatusDto request,

            Authentication authentication) {

        return ResponseEntity.ok(adminCRMService.updateClientStatus(clientId, request, authentication));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<AdminCommentResponseDto> editComment(

            @PathVariable Long commentId,

            @Valid @RequestBody UpdateCommentRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(adminCRMService.editComment(commentId, request, authentication));
    }

    @GetMapping("/clients/status/{status}")
    public ResponseEntity<List<AdminClientResponseDto>> getClientsByStatus(@PathVariable ClientStatus status) {

        return ResponseEntity.ok(adminCRMService.getClientsByStatus(status));
    }

    @GetMapping("/clients/{clientId}/documents")
    public ResponseEntity<Page<AdminDocumentResponseDto>> getClientDocuments(

            @PathVariable Long clientId,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(adminCRMService.getClientDocuments(clientId, pageable));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<AdminDocumentResponseDto> getDocument(@PathVariable Long documentId) {

        return ResponseEntity.ok(adminCRMService.getDocument(documentId));
    }

    @GetMapping("/clients/documents/pending")
    public ResponseEntity<Page<AdminClientDocumentStatusDto>> getPendingDocumentClients(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(adminCRMService.getPendingDocumentClients(pageable));
    }

    @GetMapping("/clients/documents/submitted")
    public ResponseEntity<Page<AdminClientDocumentStatusDto>> getSubmittedDocumentClients(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(adminCRMService.getSubmittedDocumentClients(pageable));
    }

    @GetMapping("/documents/summary")
    public ResponseEntity<AdminDocumentSummaryDto> getDocumentSummary() {

        return ResponseEntity.ok(adminCRMService.getDocumentSummary());
    }

    @GetMapping("/employees/{employeeId}/clients")
    public ResponseEntity<Page<AdminClientResponseDto>> getClientsByEmployee(

            @PathVariable Long employeeId,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));

        return ResponseEntity.ok(adminCRMService.getClientsByEmployee(employeeId, pageable));
    }

    @GetMapping("/clients/{clientId}/tax-organizer")
    public ResponseEntity<TaxOrganizerResponse> getTaxOrganizer(
            @PathVariable Long clientId) {

        return ResponseEntity.ok(
                taxOrganizerService.getAdminOrganizer(
                        clientId
                )
        );
    }
    @GetMapping("/clients/{clientId}/tax-organizer/view")
    public ResponseEntity<Resource> viewTaxOrganizer(
            @PathVariable Long clientId) {

        return taxOrganizerService.viewAdminOrganizer(
                clientId
        );
    }
    @GetMapping("/clients/{clientId}/tax-organizer/download")
    public ResponseEntity<Resource> downloadTaxOrganizer(
            @PathVariable Long clientId) {

        return taxOrganizerService.downloadAdminOrganizer(
                clientId
        );
    }

    @PostMapping("/clients/{clientId}/documents/approve")
    public ResponseEntity<List<AdminDocumentResponseDto>> approveAllDocuments(
            @PathVariable Long clientId,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.approveAllDocuments(
                        clientId,
                        authentication
                )
        );
    }

    @PostMapping("/clients/{clientId}/documents/reject")
    public ResponseEntity<List<AdminDocumentResponseDto>> rejectAllDocuments(
            @PathVariable Long clientId,

            @Valid @RequestBody
            DocumentReviewRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.rejectAllDocuments(
                        clientId,
                        request,
                        authentication
                )
        );
    }

    @PostMapping("/clients/{clientId}/documents/{documentId}/approve")
    public ResponseEntity<AdminDocumentResponseDto> approveDocument(

            @PathVariable Long clientId,

            @PathVariable Long documentId,

            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.approveDocument(
                        clientId,
                        documentId,
                        authentication
                )
        );
    }

    @PostMapping("/clients/{clientId}/documents/{documentId}/reject")
    public ResponseEntity<AdminDocumentResponseDto> rejectDocument(

            @PathVariable Long clientId,

            @PathVariable Long documentId,

            @Valid @RequestBody
            DocumentReviewRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.rejectDocument(
                        clientId,
                        documentId,
                        request,
                        authentication
                )
        );
    }
    @GetMapping("/clients/{clientId}/history")
    public ResponseEntity<List<AssignmentResponseDto>> getClientAssignmentHistory(
            @PathVariable Long clientId) {

        List<AssignmentResponseDto> history = adminCRMService.getClientAssignmentHistory(clientId);
        return ResponseEntity.ok(history);
    }


    @GetMapping("/documents/verified")
    public ResponseEntity<Page<AdminClientDocumentStatusDto>> getVerifiedDocumentClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminClientDocumentStatusDto> result = adminCRMService.getVerifiedDocumentClients(pageable);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/unassigned")
    public ResponseEntity<Page<AdminClientResponseDto>> getUnassignedClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminCRMService.getUnassignedClients(pageable));
    }

}