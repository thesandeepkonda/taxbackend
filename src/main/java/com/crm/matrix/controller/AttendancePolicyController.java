package com.crm.matrix.controller;

import com.crm.matrix.dto.AttendancePolicyResponse;
import com.crm.matrix.dto.CreateAttendancePolicyRequest;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.AttendancePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance-policies")
@RequiredArgsConstructor
public class AttendancePolicyController {

    private final AttendancePolicyService attendancePolicyService;

    @PostMapping
    @HasPermission("ATTENDANCE_POLICY_CREATE")
    public ResponseEntity<AttendancePolicyResponse> createPolicy(@Valid @RequestBody CreateAttendancePolicyRequest request) {

        AttendancePolicyResponse response = attendancePolicyService.createPolicy(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    @HasPermission("ATTENDANCE_POLICY_READ")
    public ResponseEntity<List<AttendancePolicyResponse>> getAllPolicies() {

        return ResponseEntity.ok(attendancePolicyService.getAllPolicies());
    }


    @GetMapping("/{id}")
    @HasPermission("ATTENDANCE_POLICY_READ")
    public ResponseEntity<AttendancePolicyResponse> getPolicyById(@PathVariable Long id) {

        return ResponseEntity.ok(attendancePolicyService.getPolicyById(id));
    }


    @PatchMapping("/{id}/deactivate")
    @HasPermission("ATTENDANCE_POLICY_UPDATE")
    public ResponseEntity<Void> deactivatePolicy(@PathVariable Long id) {

        attendancePolicyService.deactivatePolicy(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @HasPermission("ATTENDANCE_POLICY_UPDATE")
    public ResponseEntity<Void> activatePolicy(@PathVariable Long id) {

        attendancePolicyService.activatePolicy(id);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/status")
    @HasPermission("ATTENDANCE_POLICY_READ")
    public ResponseEntity<List<AttendancePolicyResponse>> getPoliciesByStatus(
            @RequestParam boolean active) {
        List<AttendancePolicyResponse> policies = active
                ? attendancePolicyService.getActivePolicies()
                : attendancePolicyService.getInactivePolicies();
        return ResponseEntity.ok(policies);
    }
}