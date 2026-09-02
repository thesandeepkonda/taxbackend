package com.crm.matrix.controller;

import com.crm.matrix.dto.*;

import com.crm.matrix.service.DocEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

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
    public ResponseEntity<List<DocClientResponseDto>> getMyClients(
            Authentication authentication) {

        return ResponseEntity.ok(
                docEmployeeService.getMyClients(
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
}