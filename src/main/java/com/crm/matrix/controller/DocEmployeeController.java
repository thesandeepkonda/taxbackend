package com.crm.matrix.controller;

import com.crm.matrix.dto.*;

import com.crm.matrix.entity.ClientComment;
import com.crm.matrix.enums.AssignmentPeriod;
import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.service.DocEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('EMPLOYEE')")
public class DocEmployeeController {

    private final DocEmployeeService docEmployeeService;


    // =====================================================
    // MY ASSIGNED CLIENTS
    // =====================================================
    @GetMapping("/clients")
    public ResponseEntity<Page<DocClientResponseDto>> getMyClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "assignedAt"
                        )
                );

        return ResponseEntity.ok(
                docEmployeeService.getMyClients(
                        pageable,
                        authentication
                )
        );
    }


    // =====================================================
    // GET ONE CLIENT
    // =====================================================

    @GetMapping("/clients/{assignmentId}")
    public ResponseEntity<DocClientResponseDto> getMyClient(
            @PathVariable Long assignmentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getMyClient(
                        assignmentId,
                        authentication
                )
        );
    }


    // =====================================================
    // UPDATE STATUS / REMARKS / FOLLOW-UP
    // =====================================================

    @PutMapping("/clients/{assignmentId}")
    public ResponseEntity<DocClientResponseDto> updateClient(
            @PathVariable Long assignmentId,

            @Valid @RequestBody
            UpdateDocClientDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.updateClient(
                        assignmentId,
                        request,
                        authentication
                )
        );
    }


    // =====================================================
    // START CALL
    // =====================================================

    @PostMapping("/clients/{assignmentId}/calls/start")
    public ResponseEntity<DocCallResponseDto> startCall(

            @PathVariable Long assignmentId,

            @RequestBody
            StartCallRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.startCall(
                        assignmentId,
                        request,
                        authentication
                )
        );
    }


    // =====================================================
    // END CALL
    // =====================================================

    @PostMapping("/calls/{callId}/end")
    public ResponseEntity<DocCallResponseDto> endCall(

            @PathVariable Long callId,

            @RequestBody
            EndCallRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.endCall(
                        callId,
                        request,
                        authentication
                )
        );
    }


    // =====================================================
    // FOLLOW UPS
    // =====================================================

    @GetMapping("/follow-ups")
    public ResponseEntity<List<DocClientResponseDto>>
    getFollowUps(Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getMyFollowUps(
                        authentication
                )
        );
    }


    // =====================================================
    // NOT LIFTED
    // =====================================================

    @GetMapping("/not-lifted")
    public ResponseEntity<List<DocClientResponseDto>>
    getNotLifted(Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getNotLifted(
                        authentication
                )
        );
    }


    // =====================================================
    // CALL HISTORY
    // =====================================================

    @GetMapping("/calls")
    public ResponseEntity<List<DocCallResponseDto>>
    getMyCalls(Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getMyCalls(
                        authentication
                )
        );
    }


    // =====================================================
    // CLIENT CALL HISTORY
    // =====================================================

    @GetMapping("/clients/{clientId}/calls")
    public ResponseEntity<List<DocCallResponseDto>>
    getClientCalls(

            @PathVariable Long clientId,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getClientCalls(
                        clientId,
                        authentication
                )
        );
    }


    @PostMapping("/comments")
    public ResponseEntity<AdminCommentResponseDto> addComment(
            @RequestBody CreateCommentRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.addComment(dto, authentication)
        );
    }
    @PutMapping("/clients/{assignmentId}/status")
    public ResponseEntity<DocClientResponseDto> updateStatus(

            @PathVariable Long assignmentId,

            @Valid @RequestBody
            UpdateClientStatusDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.updateStatus(
                        assignmentId,
                        request,
                        authentication
                )
        );
    }

    @GetMapping("/clients/{clientId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getClientComments(

            @PathVariable Long clientId,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getClientComments(
                        clientId,
                        authentication
                )
        );
    }
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<AdminCommentResponseDto> editComment(

            @PathVariable Long commentId,

            @Valid @RequestBody
            UpdateCommentRequestDto request,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.editComment(
                        commentId,
                        request,
                        authentication
                )
        );
    }
    @GetMapping("/clients/status/{status}")
    public ResponseEntity<List<DocClientResponseDto>> getMyClientsByStatus(
            @PathVariable ClientStatus status,
            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getMyClientsByStatus(
                        status,
                        authentication
                )
        );
    }

    @GetMapping("/clients/search")
    public ResponseEntity<Page<DocClientResponseDto>> searchClients(

            @RequestParam(required = false)
            Long clientId,

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            AssignmentPeriod period,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            Authentication authentication) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "assignedAt"
                        )
                );

        return ResponseEntity.ok(
                docEmployeeService.searchClients(
                        clientId,
                        name,
                        period,
                        fromDate,
                        toDate,
                        pageable,
                        authentication
                )
        );
    }

    @GetMapping("/clients/{clientId}/documents")
    public ResponseEntity<List<DocDocumentResponseDto>>
    getClientDocuments(

            @PathVariable Long clientId,

            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getClientDocuments(
                        clientId,
                        authentication
                )
        );
    }

}