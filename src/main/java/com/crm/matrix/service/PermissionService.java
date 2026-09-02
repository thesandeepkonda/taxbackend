package com.crm.matrix.service;

import com.crm.matrix.dto.CreatePermissionRequest;
import com.crm.matrix.dto.PermissionResponse;
import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.Role;
import com.crm.matrix.repository.PermissionRepository;
import com.crm.matrix.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;


    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {

        String code = request.getCode().trim().toUpperCase();

        if (permissionRepository.existsByCode(code)) {

            throw new IllegalArgumentException("Permission already exists: " + code);
        }



        Permission permission = new Permission();

        permission.setCode(code);
        permission.setActive(true);


        Permission saved = permissionRepository.save(permission);


        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        adminRole.getPermissions().add(saved);

        roleRepository.save(adminRole);


        return PermissionResponse.builder().id(saved.getId()).code(saved.getCode()).active(saved.getActive()).build();
    }


    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {

        return permissionRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {

        Permission permission = permissionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));

        return mapToResponse(permission);
    }


    private PermissionResponse mapToResponse(Permission permission) {

        return PermissionResponse.builder().id(permission.getId()).code(permission.getCode()).active(permission.getActive()).build();
    }
}