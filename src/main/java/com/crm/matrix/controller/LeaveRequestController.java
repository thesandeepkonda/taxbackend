package com.crm.matrix.controller;

import com.crm.matrix.dto.CreateLeaveRequest;
import com.crm.matrix.dto.LeaveRequestResponse;
import com.crm.matrix.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;


    // =========================================================
    // EMPLOYEE - APPLY LEAVE
    // =========================================================

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(Authentication authentication, @Valid @RequestBody CreateLeaveRequest request) {

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // =========================================================
    // EMPLOYEE - MY LEAVE REQUESTS
    // =========================================================

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests(Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.getMyLeaveRequests(authentication.getName()));
    }


    // =========================================================
    // ADMIN - PENDING REQUESTS
    // =========================================================

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaveRequests() {

        return ResponseEntity.ok(leaveRequestService.getPendingLeaveRequests());
    }


    // =========================================================
    // ADMIN - APPROVE
    // =========================================================

    @PutMapping("/{leaveId}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeave(@PathVariable Long leaveId,

                                                             @RequestParam(required = false) String remark,

                                                             Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.approveLeave(leaveId, authentication.getName(), remark));
    }


    // =========================================================
    // ADMIN - REJECT
    // =========================================================

    @PutMapping("/{leaveId}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(@PathVariable Long leaveId,

                                                            @RequestParam(required = false) String remark,

                                                            Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.rejectLeave(leaveId, authentication.getName(), remark));
    }
}