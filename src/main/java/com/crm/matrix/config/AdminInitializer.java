package com.crm.matrix.config;

import com.crm.matrix.entity.AttendancePolicy;
import com.crm.matrix.entity.Department;
import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.Role;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.WorkMode;
import com.crm.matrix.repository.AttendancePolicyRepository;
import com.crm.matrix.repository.DepartmentRepository;
import com.crm.matrix.repository.PermissionRepository;
import com.crm.matrix.repository.RoleRepository;
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

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final DepartmentRepository departmentRepository;

    private final AttendancePolicyRepository attendancePolicyRepository;

    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public void run(String... args) {

        // =========================================================
        // 1. CREATE / GET ADMIN ROLE
        // =========================================================

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {

            Role role = new Role();

            role.setName("ADMIN");

            role.setDescription("System Administrator");

            role.setActive(true);

            role.setPermissions(new HashSet<>());

            System.out.println("ADMIN role created.");

            return roleRepository.save(role);
        });


        // =========================================================
        // 2. SYNCHRONIZE ALL PERMISSIONS TO ADMIN
        // =========================================================

        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());


        Set<Permission> adminPermissions = adminRole.getPermissions();


        if (adminPermissions == null) {

            adminPermissions = new HashSet<>();

            adminRole.setPermissions(adminPermissions);
        }


        boolean permissionsChanged = false;


        for (Permission permission : allPermissions) {

            if (permission == null) {
                continue;
            }


            if (!adminPermissions.contains(permission)) {

                adminPermissions.add(permission);

                permissionsChanged = true;
            }
        }


        if (permissionsChanged) {

            roleRepository.save(adminRole);

            System.out.println("ADMIN permissions synchronized.");
        }


        // =========================================================
        // 3. CREATE / GET SYSTEM DEPARTMENT
        // =========================================================

        Department systemDepartment = departmentRepository.findByNameIgnoreCase("SYSTEM").orElseGet(() -> {

            Department department = new Department();

            department.setName("SYSTEM");

            department.setDescription("System administration department");

            department.setActive(true);

            System.out.println("SYSTEM department created.");

            return departmentRepository.save(department);
        });


        // =========================================================
        // 4. CREATE / GET SYSTEM ATTENDANCE POLICY
        // =========================================================

        AttendancePolicy systemAttendancePolicy = attendancePolicyRepository.findByNameIgnoreCase("SYSTEM_ADMIN_POLICY").orElseGet(() -> {

            AttendancePolicy policy = new AttendancePolicy();

            policy.setName("SYSTEM_ADMIN_POLICY");

            policy.setStartTime(LocalTime.of(0, 0));

            policy.setEndTime(LocalTime.of(23, 59));

            policy.setActive(true);

            System.out.println("SYSTEM_ADMIN_POLICY created.");

            return attendancePolicyRepository.save(policy);
        });


        // =========================================================
        // 5. CREATE / GET ADMIN USER
        // =========================================================

        String adminEmployeeCode = "ADMIN001";


        User admin = userRepository.findByEmployeeCode(adminEmployeeCode).orElse(null);


        // =========================================================
        // 6. CREATE ADMIN IF NOT EXISTS
        // =========================================================

        if (admin == null) {

            admin = new User();

            admin.setEmployeeCode(adminEmployeeCode);

            admin.setFirstName("System");

            admin.setLastName("Administrator");

            admin.setEmail("admin@matrix.com");

            admin.setPhone("9999999999");


            // -----------------------------------------------------
            // INITIAL PASSWORD
            // -----------------------------------------------------

            String initialPassword = "Admin@12345";


            admin.setPassword(passwordEncoder.encode(initialPassword));


            admin.setActive(true);


            System.out.println("Creating initial ADMIN user...");
        }


        // =========================================================
        // 7. ADMIN ORGANIZATION
        // =========================================================

        admin.setRole(adminRole);


        admin.setDepartment(systemDepartment);


        // ADMIN does not belong to a normal team

        admin.setTeam(null);


        // =========================================================
        // 8. ADMIN ATTENDANCE CONFIGURATION
        // =========================================================

        admin.setAttendancePolicy(systemAttendancePolicy);


        /*
         * ADMIN is treated as an office/system user.
         * This value is required because work_mode is
         * nullable = false in User.
         */

        admin.setWorkMode(WorkMode.OFFICE);


        // =========================================================
        // 9. SAVE ADMIN
        // =========================================================

        userRepository.save(admin);


        // =========================================================
        // 10. LOG
        // =========================================================

        System.out.println("==============================================");

        System.out.println("Initial ADMIN configuration completed.");

        System.out.println("Employee ID : ADMIN001");

        System.out.println("Email       : admin@matrix.com");

        System.out.println("Department  : SYSTEM");

        System.out.println("Role        : ADMIN");

        System.out.println("Team        : NONE");

        System.out.println("Attendance  : SYSTEM_ADMIN_POLICY");

        System.out.println("Work Mode   : OFFICE");

        System.out.println("==============================================");
    }
}