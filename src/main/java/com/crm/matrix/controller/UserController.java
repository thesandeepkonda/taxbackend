package com.crm.matrix.controller;

import com.crm.matrix.dto.*;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            Authentication authentication) {

        userService.quickAssign(id, request, authentication.getName());

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


}