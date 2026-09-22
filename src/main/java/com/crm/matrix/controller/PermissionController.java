//package com.crm.matrix.controller;
//
//import com.crm.matrix.dto.CreatePermissionRequest;
//import com.crm.matrix.dto.PermissionResponse;
//import com.crm.matrix.security.HasPermission;
////import com.crm.matrix.service.PermissionService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/permissions")
//@RequiredArgsConstructor
//public class PermissionController {
//
//    private final PermissionService permissionService;
//
//
//    @HasPermission("PERMISSION_CREATE")
//    @PostMapping
//    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
//
//        PermissionResponse response = permissionService.createPermission(request);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @HasPermission("PERMISSION_READ")
//    @GetMapping
//    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
//
//        return ResponseEntity.ok(permissionService.getAllPermissions());
//    }
//
//
//    @HasPermission("PERMISSION_READ")
//    @GetMapping("/{id}")
//    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable Long id) {
//
//        return ResponseEntity.ok(permissionService.getPermissionById(id));
//    }
//}