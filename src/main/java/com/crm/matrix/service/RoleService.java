//package com.crm.matrix.service;
//
//import com.crm.matrix.dto.CreateRoleRequest;
//import com.crm.matrix.dto.PermissionResponse;
//import com.crm.matrix.dto.RoleResponse;
//import com.crm.matrix.entity.Role;
//import com.crm.matrix.repository.RoleRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class RoleService {
//
//    private final RoleRepository roleRepository;
//
//
//    @Transactional
//    public RoleResponse createRole(CreateRoleRequest request) {
//
//        String roleName = request.getName().trim().toUpperCase();
//
//        if (roleRepository.existsByName(roleName)) {
//
//            throw new IllegalArgumentException("Role already exists: " + roleName);
//        }
//
//        Role role = new Role();
//
//        role.setName(roleName);
//
//        if (request.getDescription() != null) {
//
//            role.setDescription(request.getDescription().trim());
//        }
//
//        role.setActive(true);
//
//        role.setPermissions(new java.util.HashSet<>());
//
//        Role savedRole = roleRepository.save(role);
//
//        return RoleResponse.builder().id(savedRole.getId()).name(savedRole.getName()).description(savedRole.getDescription()).active(savedRole.getActive()).build();
//    }
//
//
//    @Transactional(readOnly = true)
//    public List<RoleResponse> getAllRoles() {
//
//        return roleRepository.findAll().stream().map(this::mapToResponse).toList();
//    }
//
//
//    @Transactional(readOnly = true)
//    public RoleResponse getRoleById(Long id) {
//
//        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
//
//        return mapToResponse(role);
//    }
//
//
//    private RoleResponse mapToResponse(Role role) {
//
//        return RoleResponse.builder().id(role.getId()).name(role.getName()).description(role.getDescription()).active(role.getActive()).build();
//    }
//    @Transactional(readOnly = true)
//    public List<PermissionResponse> getPermissionsByRoleId(Long id) {
//        Role role = roleRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
//
//        if (role.getPermissions() == null) {
//            return List.of();
//        }
//
//        return role.getPermissions().stream()
//                .map(permission -> PermissionResponse.builder()
//                        .id(permission.getId())
//                        .code(permission.getCode())
//                        .active(permission.getActive())
//                        .build())
//                .toList();
//    }
//}