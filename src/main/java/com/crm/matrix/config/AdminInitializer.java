package com.crm.matrix.config;

import com.crm.matrix.entity.AttendancePolicy;
import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.Role;
import com.crm.matrix.enums.WorkMode;
import com.crm.matrix.repository.AttendancePolicyRepository;
import com.crm.matrix.repository.PermissionRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(2)
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        // =========================================================
        // 1. CREATE / GET SYSTEM ATTENDANCE POLICY
        // =========================================================
        AttendancePolicy systemAttendancePolicy = attendancePolicyRepository.findByNameIgnoreCase("SYSTEM_ADMIN_POLICY").orElseGet(() -> {
            AttendancePolicy policy = new AttendancePolicy();
            policy.setName("SYSTEM_ADMIN_POLICY");
            policy.setStartTime(LocalTime.of(0, 0));
            policy.setEndTime(LocalTime.of(23, 59));
            policy.setActive(true);
            policy.setAllowedBreakMinutes(60);
            policy.setWorkingDays("Monday-Friday");
            System.out.println("SYSTEM_ADMIN_POLICY created.");
            return attendancePolicyRepository.save(policy);
        });

        // =========================================================
        // 2. CREATE / GET ADMIN USER
        // =========================================================
        String adminEmployeeCode = "ADMIN001";
        User admin = userRepository.findByEmployeeCode(adminEmployeeCode).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setEmployeeCode(adminEmployeeCode);
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail("admin@matrix.com");
            admin.setPhone("9999999999");
            admin.setPassword(passwordEncoder.encode("Admin@12345"));
            admin.setActive(true);
            System.out.println("Creating initial ADMIN user...");
        }

        // =========================================================
        // 3. ADMIN ORGANIZATION & CONFIGURATION
        // =========================================================
        admin.setRole(Role.ADMIN);
        admin.setDepartment(null);
        admin.setTeam(null);
        admin.setAttendancePolicy(systemAttendancePolicy);
        admin.setWorkMode(WorkMode.OFFICE);

        // =========================================================
        // 4. AUTO-INITIALIZE ALL PERMISSIONS TO ADMIN
        // =========================================================
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
        if (admin.getPermissions() == null) {
            admin.setPermissions(new HashSet<>());
        }

        // Add all system permissions to the Admin user
        admin.getPermissions().addAll(allPermissions);

        // =========================================================
        // 5. SAVE ADMIN
        // =========================================================
        userRepository.save(admin);

        System.out.println("==============================================");
        System.out.println("Initial ADMIN configuration & permissions sync completed.");
        System.out.println("Total permissions granted to ADMIN: " + allPermissions.size());
        System.out.println("==============================================");
    }
}