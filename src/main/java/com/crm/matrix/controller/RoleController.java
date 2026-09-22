//package com.crm.matrix.controller;
//
//import com.crm.matrix.dto.AssignPermissionsRequest;
//import com.crm.matrix.dto.CreateRoleRequest;
//import com.crm.matrix.dto.PermissionResponse;
//import com.crm.matrix.dto.RoleResponse;
//import com.crm.matrix.security.HasPermission;
//import com.crm.matrix.service.RolePermissionService;
//import com.crm.matrix.service.RoleService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/roles")
//@RequiredArgsConstructor
//public class RoleController {
//
//    private final RoleService roleService;
//
//    private final RolePermissionService rolePermissionService;
//
//
//    @HasPermission("ROLE_CREATE")
//    @PostMapping
//    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
//
//        RoleResponse response = roleService.createRole(request);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//
//    @HasPermission("ROLE_READ")
//    @GetMapping
//    public ResponseEntity<List<RoleResponse>> getAllRoles() {
//
//        return ResponseEntity.ok(roleService.getAllRoles());
//    }
//
//
//    @HasPermission("ROLE_READ")
//    @GetMapping("/{id}")
//    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
//
//        return ResponseEntity.ok(roleService.getRoleById(id));
//    }
//
//    @HasPermission("ROLE_PERMISSION_ASSIGN")
//    @PutMapping("/{roleId}/permissions")
//    public ResponseEntity<Void> assignPermissions(@PathVariable Long roleId, @Valid @RequestBody AssignPermissionsRequest request) {
//
//        rolePermissionService.assignPermissions(roleId, request.getPermissionIds());
//
//        return ResponseEntity.noContent().build();
//    }
//
//    @HasPermission("ROLE_READ")
//    @GetMapping("/{id}/permissions")
//    public ResponseEntity<List<PermissionResponse>> getPermissionsByRoleId(
//            @PathVariable Long id
//    ) {
//        return ResponseEntity.ok(
//                roleService.getPermissionsByRoleId(id)
//        );
//    }
//}