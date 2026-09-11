package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @HasPermission("USER_CREATE")
    @PostMapping
    public ResponseEntity<CreateEmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {

        CreateEmployeeResponse response = userService.createEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @HasPermission("PASSWORD_CHANGE")
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {

        userService.changePassword(authentication.getName(), request);

        return ResponseEntity.noContent().build();
    }

    @HasPermission("USER_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<CreateEmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.updateEmployee(id, request, authentication.getName()));
    }

    @HasPermission("USER_READ")
    @GetMapping
    public ResponseEntity<List<CreateEmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(userService.getAllEmployees());
    }

    @HasPermission("USER_READ")
    @GetMapping("/{id}")
    public ResponseEntity<CreateEmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getEmployeeById(id));
    }

    @HasPermission("USER_UPDATE")
    @PatchMapping("/{id}/quick-assign")
    public ResponseEntity<MessageResponseDto> quickAssign(
            @PathVariable Long id,
            @RequestBody QuickAssignRequestDto request,
            @RequestParam(defaultValue = "false") boolean override,
            Authentication authentication) {

        userService.quickAssign(id, request, authentication.getName(), override);

        return ResponseEntity.ok(MessageResponseDto.builder()
                .success(true)
                .message("Assignment updated successfully")
                .build());
    }

    @HasPermission("USER_READ")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ActivityLogResponseDto>> getEmployeeHistory(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getEmployeeHistory(id));
    }

    @HasPermission("USER_READ")
    @GetMapping("/team-leads")
    public ResponseEntity<List<TeamLeadResponseDto>> getAllTeamLeads() {
        return ResponseEntity.ok(userService.getAllTeamLeadsWithDepartment());
    }
    @HasPermission("USER_READ")
    @GetMapping("/status")
    public ResponseEntity<Page<CreateEmployeeResponse>> getUsersByStatus(
            @RequestParam boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getUsersByStatus(active, pageable));
    }

    @HasPermission("USER_UPDATE")
    @PatchMapping("/{id}/status")
    public ResponseEntity<MessageResponseDto> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication) {
        userService.updateUserStatus(id, active, authentication.getName());
        return ResponseEntity.ok(MessageResponseDto.builder()
                .success(true)
                .message(active ? "User activated successfully" : "User deactivated successfully")
                .build());
    }
    @PutMapping("/employees/{id}/reset-password")
    public ResponseEntity<Map<String, String>> adminResetPassword(
            @PathVariable Long id,
            Authentication authentication) {
        String temporaryPassword = userService.adminResetPassword(id, authentication.getName());
        return ResponseEntity.ok(Map.of(
                "temporaryPassword", temporaryPassword,
                "message", "Password reset successfully. Provide this temporary password to the employee."
        ));
    }


}