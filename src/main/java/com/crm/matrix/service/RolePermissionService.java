package com.crm.matrix.service;

import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.Role;
import com.crm.matrix.repository.PermissionRepository;
import com.crm.matrix.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public void assignPermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        Set<Permission> newPermissions = new HashSet<>(permissionRepository.findAllById(permissionIds));

        if (newPermissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permission IDs are invalid");
        }

        role.getPermissions().addAll(newPermissions);

        roleRepository.save(role);
    }
}