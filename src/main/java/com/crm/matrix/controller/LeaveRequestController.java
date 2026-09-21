package com.crm.matrix.controller;

import com.crm.matrix.dto.CreateLeaveRequest;
import com.crm.matrix.dto.LeaveRequestResponse;
import com.crm.matrix.security.HasPermission;
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



    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(Authentication authentication, @Valid @RequestBody CreateLeaveRequest request) {

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests(Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.getMyLeaveRequests(authentication.getName()));
    }


    @GetMapping("/pending")
    @HasPermission("LEAVE_REQUEST")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaveRequests() {

        return ResponseEntity.ok(leaveRequestService.getPendingLeaveRequests());
    }

    @PutMapping("/{leaveId}/approve")
    @HasPermission("LEAVE_REQUEST")

    public ResponseEntity<LeaveRequestResponse> approveLeave(@PathVariable Long leaveId,

                                                             @RequestParam(required = false) String remark,

                                                             Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.approveLeave(leaveId, authentication.getName(), remark));
    }


    @PutMapping("/{leaveId}/reject")
    @HasPermission("LEAVE_REQUEST")

    public ResponseEntity<LeaveRequestResponse> rejectLeave(@PathVariable Long leaveId,

                                                            @RequestParam(required = false) String remark,

                                                            Authentication authentication) {

        return ResponseEntity.ok(leaveRequestService.rejectLeave(leaveId, authentication.getName(), remark));
    }

    @GetMapping("/admin/on-leave-today")
    @HasPermission("LEAVE_REQUEST")
    public ResponseEntity<List<LeaveRequestResponse>> getAllEmployeesOnLeaveToday() {
        return ResponseEntity.ok(leaveRequestService.getAllEmployeesOnLeaveToday());
    }

    // TEAM LEAD ENDPOINT
    @GetMapping("/team/on-leave-today")
    public ResponseEntity<List<LeaveRequestResponse>> getMyTeamMembersOnLeaveToday(Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.getMyTeamMembersOnLeaveToday(authentication.getName()));
    }
}