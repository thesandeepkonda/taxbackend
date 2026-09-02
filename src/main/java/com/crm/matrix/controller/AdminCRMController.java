package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.service.AdminCRMService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @PostMapping(
            value = "/client-imports/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ClientImportResponseDto> uploadExcel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        System.out.println("========== EXCEL UPLOAD ==========");
        System.out.println("File: " + file.getOriginalFilename());
        System.out.println("Size: " + file.getSize());
        System.out.println("User: " + authentication.getName());

        return ResponseEntity.ok(
                adminCRMService.uploadExcel(
                        file,
                        authentication
                )
        );
    }


    @GetMapping("/clients")
    public ResponseEntity<Page<AdminClientResponseDto>>
    getClients(Pageable pageable) {

        return ResponseEntity.ok(
                adminCRMService.getClients(
                        pageable
                )
        );
    }



    @GetMapping("/clients/search")
    public ResponseEntity<Page<AdminClientResponseDto>>
    searchClients(
            @RequestParam String name,
            Pageable pageable) {

        return ResponseEntity.ok(
                adminCRMService.searchByName(
                        name,
                        pageable
                )
        );
    }



    @GetMapping("/clients/{clientId}")
    public ResponseEntity<AdminClientResponseDto>
    getClient(
            @PathVariable Long clientId) {

        return ResponseEntity.ok(
                adminCRMService.getClient(
                        clientId
                )
        );
    }




    @PostMapping("/assignments/bulk")
    public ResponseEntity<List<AssignmentResponseDto>> bulkAssign(
            @Valid @RequestBody
            BulkAssignClientRequestDto request,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.bulkAssign(
                        request,
                        authentication
                )
        );
    }


    @PostMapping(
            "/assignments/{assignmentId}/reassign"
    )
    public ResponseEntity<AssignmentResponseDto>
    reassign(
            @PathVariable Long assignmentId,

            @Valid @RequestBody
            ReassignClientRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.reassign(
                        assignmentId,
                        request,
                        authentication
                )
        );
    }




    @GetMapping("/clients/follow-ups")
    public ResponseEntity<List<AssignmentResponseDto>> getFollowUps() {

        return ResponseEntity.ok(
                adminCRMService.getFollowUps()
        );
    }



    @GetMapping("/clients/not-lifted")
    public ResponseEntity<List<AssignmentResponseDto>>
    getNotLifted() {

        return ResponseEntity.ok(
                adminCRMService.getNotLifted()
        );
    }




    @GetMapping("/calls")
    public ResponseEntity<Page<AdminCallResponseDto>> getCalls(Pageable pageable) {

        return ResponseEntity.ok(
                adminCRMService.getCalls(
                        pageable
                )
        );
    }


    @GetMapping("/clients/{clientId}/calls")
    public ResponseEntity<Page<AdminCallResponseDto>> getClientCalls(
            @PathVariable Long clientId,
            Pageable pageable) {

        return ResponseEntity.ok(
                adminCRMService.getClientCalls(
                        clientId,
                        pageable
                )
        );
    }


    @GetMapping("/calls/{callId}/recording")
    public ResponseEntity<AdminCallResponseDto> getRecording(
            @PathVariable Long callId) {

        return ResponseEntity.ok(
                adminCRMService.getRecording(
                        callId
                )
        );
    }



    @GetMapping("/clients/{clientId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long clientId) {

        return ResponseEntity.ok(
                adminCRMService.getClientComments(
                        clientId
                )
        );
    }


    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        adminCRMService.deleteComment(
                commentId,
                authentication
        );

        return ResponseEntity.noContent()
                .build();
    }




    @GetMapping(
            "/reports/employees/{employeeId}"
    )
    public ResponseEntity<EmployeeCallReportDto> employeeReport(

            @PathVariable Long employeeId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to) {

        return ResponseEntity.ok(
                adminCRMService.getEmployeeReport(
                        employeeId,
                        from,
                        to
                )
        );
    }

    @PostMapping("/assignments/bulk-reassign")
    public ResponseEntity<List<AssignmentResponseDto>> bulkReassign(
            @Valid @RequestBody
            BulkReassignClientRequestDto request,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminCRMService.bulkReassign(
                        request,
                        authentication
                )
        );
    }
}