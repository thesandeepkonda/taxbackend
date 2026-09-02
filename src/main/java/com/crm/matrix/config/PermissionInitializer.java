package com.crm.matrix.config;

import com.crm.matrix.entity.Permission;
import com.crm.matrix.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {

        List<String> permissions = List.of(

                // USER
                "USER_CREATE", "USER_READ", "USER_UPDATE", "USER_DEACTIVATE",

                // ROLE
                "ROLE_CREATE", "ROLE_READ", "ROLE_UPDATE", "ROLE_DELETE",

                // PERMISSION
                "PERMISSION_CREATE", "PERMISSION_READ",

                // ROLE PERMISSION
                "ROLE_PERMISSION_ASSIGN",

                // DEPARTMENT
                "DEPARTMENT_CREATE", "DEPARTMENT_READ", "DEPARTMENT_UPDATE",

                // TEAM
                "TEAM_CREATE", "TEAM_READ", "TEAM_UPDATE", "TEAM_ASSIGN",

                // CLIENT
                "CLIENT_CREATE", "CLIENT_READ", "CLIENT_UPDATE", "CLIENT_ASSIGN",

                // ASSIGNMENT
                "ASSIGNMENT_CREATE", "ASSIGNMENT_READ", "ASSIGNMENT_UPDATE", "ASSIGNMENT_TRANSFER",

                // ATTENDANCE
                "ATTENDANCE_POLICY_UPDATE", "ATTENDANCE_POLICY_READ","ATTENDANCE_POLICY_CREATE",

                //CHANGE PASSWORD
                "PASSWORD_CHANGE",

                // AUDIT
                "AUDIT_LOG_READ");

        for (String code : permissions) {

            if (!permissionRepository.existsByCode(code)) {

                Permission permission = new Permission();

                permission.setCode(code);
                permission.setActive(true);

                permissionRepository.save(permission);
            }
        }

        System.out.println("Standard permissions initialized.");
    }
}