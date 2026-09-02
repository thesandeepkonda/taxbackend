package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final TeamRepository teamRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final ActivityLogRepository activityLogRepository;


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "@#$%&*!";

    private final SecureRandom secureRandom = new SecureRandom();


    @Transactional
    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String employeeCode = request.getEmployeeCode().trim();

        String firstName = request.getFirstName().trim();

        String email = request.getEmail().trim().toLowerCase();

        String phone = request.getPhone().trim();


        if (userRepository.existsByEmployeeCode(employeeCode)) {

            throw new IllegalArgumentException("Employee code already exists: " + employeeCode);
        }


        if (userRepository.existsByEmail(email)) {

            throw new IllegalArgumentException("Email already exists: " + email);
        }
        Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));
        if (!Boolean.TRUE.equals(department.getActive())) {

            throw new IllegalArgumentException("Cannot create employee in an inactive department");
        }
        AttendancePolicy attendancePolicy = attendancePolicyRepository.findById(request.getAttendancePolicyId()).orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + request.getAttendancePolicyId()));
        if (!Boolean.TRUE.equals(attendancePolicy.getActive())) {

            throw new IllegalArgumentException("Cannot assign an inactive attendance policy");
        }
        Team team = null;

        if (request.getTeamId() != null) {

            team = teamRepository.findById(request.getTeamId()).orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.getTeamId()));

            if (!Boolean.TRUE.equals(team.getActive())) {

                throw new IllegalArgumentException("Cannot assign an inactive team");
            }


            if (team.getDepartment() == null) {

                throw new IllegalArgumentException("Team is not assigned to a department");
            }


            if (!team.getDepartment().getId().equals(department.getId())) {

                throw new IllegalArgumentException("Selected team does not belong to selected department");
            }
        }


        Role role = null;

        if (request.getRoleId() != null) {

            role = roleRepository.findById(request.getRoleId()).orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));


            if (!Boolean.TRUE.equals(role.getActive())) {

                throw new IllegalArgumentException("Cannot assign an inactive role");
            }
        }


        if (isTeamLead(role)) {


            if (team == null) {

                throw new IllegalArgumentException("A TEAM_LEAD must be assigned to a team");
            }


            if (team.getTeamLead() != null) {

                throw new IllegalArgumentException("Team already has a team lead: " + team.getTeamLead().getEmployeeCode());
            }
        }
        User user = new User();

        user.setEmployeeCode(employeeCode);

        user.setFirstName(firstName);
        if (request.getLastName() != null && !request.getLastName().isBlank()) {

            user.setLastName(request.getLastName().trim());
        }
        user.setEmail(email);
        user.setPhone(phone);
        user.setAttendancePolicy(attendancePolicy);
        user.setWorkMode(request.getWorkMode());
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));

        user.setActive(true);


        user.setDepartment(department);

        user.setTeam(team);

        user.setRole(role);


        User savedUser = userRepository.save(user);


        if (isTeamLead(role)) {

            team.setTeamLead(savedUser);

            teamRepository.save(team);
        }


        return buildEmployeeResponse(savedUser, temporaryPassword);
    }


    private String generateTemporaryPassword() {

        int length = 12;

        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {

            int index = secureRandom.nextInt(CHARACTERS.length());

            password.append(CHARACTERS.charAt(index));
        }

        return password.toString();
    }


    @Transactional
    public void changePassword(String employeeCode, ChangePasswordRequest request) {

        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new IllegalArgumentException("Employee not found"));


        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {

            throw new IllegalArgumentException("Current password is incorrect");
        }


        if (request.getCurrentPassword().equals(request.getNewPassword())) {

            throw new IllegalArgumentException("New password must be different from current password");
        }


        if (!request.getNewPassword().equals(request.getConfirmPassword())) {

            throw new IllegalArgumentException("New password and confirm password do not match");
        }


        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Transactional
    public CreateEmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request, String adminEmployeeCode) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
        String newEmail = request.getEmail().trim().toLowerCase();

        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already exists: " + newEmail);
        }

        Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));

        if (!Boolean.TRUE.equals(department.getActive())) {
            throw new IllegalArgumentException("Cannot assign employee to an inactive department");
        }

        AttendancePolicy attendancePolicy = attendancePolicyRepository.findById(request.getAttendancePolicyId()).orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + request.getAttendancePolicyId()));

        if (!Boolean.TRUE.equals(attendancePolicy.getActive())) {
            throw new IllegalArgumentException("Cannot assign an inactive attendance policy");
        }

        Team newTeam = null;

        if (request.getTeamId() != null) {
            newTeam = teamRepository.findById(request.getTeamId()).orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.getTeamId()));

            if (!Boolean.TRUE.equals(newTeam.getActive())) {
                throw new IllegalArgumentException("Cannot assign an inactive team");
            }

            if (newTeam.getDepartment() == null || !newTeam.getDepartment().getId().equals(department.getId())) {
                throw new IllegalArgumentException("Selected team does not belong to the selected department");
            }
        }

        Role newRole = null;

        if (request.getRoleId() != null) {
            newRole = roleRepository.findById(request.getRoleId()).orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));

            if (!Boolean.TRUE.equals(newRole.getActive())) {
                throw new IllegalArgumentException("Cannot assign an inactive role");
            }
        }

        Team oldTeam = user.getTeam();
        Role oldRole = user.getRole();
        Department oldDepartment = user.getDepartment(); // Captured for audit logs

        if (isTeamLead(newRole)) {
            if (newTeam == null) {
                throw new IllegalArgumentException("A TEAM_LEAD must be assigned to a team");
            }

            if (newTeam.getTeamLead() != null && !newTeam.getTeamLead().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Team already has a team lead: " + newTeam.getTeamLead().getEmployeeCode());
            }
        }

        if (isTeamLead(oldRole) && oldTeam != null) {
            if (oldTeam.getTeamLead() != null && oldTeam.getTeamLead().getId().equals(user.getId())) {
                oldTeam.setTeamLead(null);
                teamRepository.save(oldTeam);
            }
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        user.setEmail(newEmail);
        user.setPhone(request.getPhone().trim());
        user.setDepartment(department);
        user.setTeam(newTeam);
        user.setRole(newRole);
        user.setAttendancePolicy(attendancePolicy);
        user.setWorkMode(request.getWorkMode());
        user.setActive(request.getActive());

        User savedUser = userRepository.save(user);

        if (isTeamLead(newRole)) {
            newTeam.setTeamLead(savedUser);
            teamRepository.save(newTeam);
        }

        // =========================================================
        // AUDIT LOGGING FOR FULL EDIT ENDPOINT
        // =========================================================

        // 1. Log Department Change
        if (oldDepartment != null && department != null && !oldDepartment.getId().equals(department.getId())) {
            logActivity(savedUser, adminEmployeeCode, "DEPARTMENT_CHANGED",
                    "Moved from department " + oldDepartment.getName() + " to " + department.getName());
        }

        // 2. Log Team Change
        if (request.getTeamId() != null && (oldTeam == null || !oldTeam.getId().equals(newTeam.getId()))) {
            String oldName = oldTeam != null ? oldTeam.getName() : "None";
            logActivity(savedUser, adminEmployeeCode, "TEAM_CHANGED",
                    "Moved from team " + oldName + " to " + newTeam.getName());
        } else if (request.getTeamId() == null && oldTeam != null) {
            // Edge case: Admin removes them from a team entirely
            logActivity(savedUser, adminEmployeeCode, "TEAM_CHANGED",
                    "Removed from team " + oldTeam.getName());
        }

        // 3. Log Role Change
        if (request.getRoleId() != null && (oldRole == null || !oldRole.getId().equals(newRole.getId()))) {
            String oldName = oldRole != null ? oldRole.getName() : "None";
            logActivity(savedUser, adminEmployeeCode, "ROLE_CHANGED",
                    "Changed role from " + oldName + " to " + newRole.getName());
        }

        return buildEmployeeResponse(savedUser, null);
    }


    @Transactional(readOnly = true)
    public List<CreateEmployeeResponse> getAllEmployees() {

        return userRepository.findAll().stream().map(user -> buildEmployeeResponse(user, null)).toList();
    }


    private boolean isTeamLead(Role role) {

        return role != null && "TEAM_LEAD".equalsIgnoreCase(role.getName());
    }


    private CreateEmployeeResponse buildEmployeeResponse(User user, String temporaryPassword) {

        return CreateEmployeeResponse.builder()

                .id(user.getId())

                .employeeCode(user.getEmployeeCode())

                .firstName(user.getFirstName())

                .lastName(user.getLastName())

                .email(user.getEmail())

                .phone(user.getPhone())



                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)

                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)

                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)

                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)

                .roleId(user.getRole() != null ? user.getRole().getId() : null)

                .roleName(user.getRole() != null ? user.getRole().getName() : null)

                .attendancePolicyId(user.getAttendancePolicy() != null ? user.getAttendancePolicy().getId() : null)

                .attendancePolicyName(user.getAttendancePolicy() != null ? user.getAttendancePolicy().getName() : null)


                .workMode(user.getWorkMode() != null ? user.getWorkMode().name() : null)


                // =================================================
                // ACCOUNT
                // =================================================

                .temporaryPassword(temporaryPassword)

                .active(user.getActive())

                .build();
    }

    @Transactional(readOnly = true)
    public CreateEmployeeResponse getEmployeeById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        return buildEmployeeResponse(user, null);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponseDto> getEmployeeHistory(Long userId) {
        // Ensure user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + userId));

        // Fetch and map to DTO
        return activityLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(log -> ActivityLogResponseDto.builder()
                        .id(log.getId())
                        .performedBy(log.getPerformedBy())
                        .actionType(log.getActionType())
                        .description(log.getDescription())
                        .timestamp(log.getCreatedAt()) // Inherited from BaseEntity
                        .build())
                .toList();
    }


    @Transactional
    public void quickAssign(Long userId, QuickAssignRequestDto request, String adminEmployeeCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + userId));

        // Store old values for comparison
        Department oldDepartment = user.getDepartment();
        Team oldTeam = user.getTeam();
        Role oldRole = user.getRole();

        boolean isModified = false;

        // =========================================================
        // 1. UPDATE DEPARTMENT
        // =========================================================
        if (request.getDepartmentId() != null &&
                (oldDepartment == null || !oldDepartment.getId().equals(request.getDepartmentId()))) {

            Department newDepartment = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            if (!Boolean.TRUE.equals(newDepartment.getActive())) {
                throw new IllegalArgumentException("Cannot assign to an inactive department");
            }

            user.setDepartment(newDepartment);
            logActivity(user, adminEmployeeCode, "DEPARTMENT_CHANGED",
                    "Moved from " + (oldDepartment != null ? oldDepartment.getName() : "None") +
                            " to " + newDepartment.getName());
            isModified = true;
        }

        // =========================================================
        // 2. UPDATE TEAM
        // =========================================================
        if (request.getTeamId() != null &&
                (oldTeam == null || !oldTeam.getId().equals(request.getTeamId()))) {

            Team newTeam = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));

            if (!Boolean.TRUE.equals(newTeam.getActive())) {
                throw new IllegalArgumentException("Cannot assign to an inactive team");
            }
            // Ensure the new team belongs to the user's current (or newly set) department
            if (!newTeam.getDepartment().getId().equals(user.getDepartment().getId())) {
                throw new IllegalArgumentException("Team does not belong to the employee's department");
            }

            // Handle old team lead cleanup if necessary
            if (isTeamLead(oldRole) && oldTeam != null && oldTeam.getTeamLead() != null && oldTeam.getTeamLead().getId().equals(user.getId())) {
                oldTeam.setTeamLead(null);
                teamRepository.save(oldTeam);
            }

            user.setTeam(newTeam);
            logActivity(user, adminEmployeeCode, "TEAM_CHANGED",
                    "Moved from team " + (oldTeam != null ? oldTeam.getName() : "None") +
                            " to " + newTeam.getName());
            isModified = true;
        }

        // =========================================================
        // 3. UPDATE ROLE
        // =========================================================
        if (request.getRoleId() != null &&
                (oldRole == null || !oldRole.getId().equals(request.getRoleId()))) {

            Role newRole = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));

            if (!Boolean.TRUE.equals(newRole.getActive())) {
                throw new IllegalArgumentException("Cannot assign an inactive role");
            }

            user.setRole(newRole);
            logActivity(user, adminEmployeeCode, "ROLE_CHANGED",
                    "Changed role from " + (oldRole != null ? oldRole.getName() : "None") +
                            " to " + newRole.getName());
            isModified = true;
        }

        // =========================================================
        // 4. SAVE CHANGES & TEAM LEAD LOGIC
        // =========================================================
        if (isModified) {
            User savedUser = userRepository.save(user);

            // If the user was just made a team lead, or moved to a new team as a team lead
            if (isTeamLead(savedUser.getRole()) && savedUser.getTeam() != null) {
                Team currentTeam = savedUser.getTeam();
                if (currentTeam.getTeamLead() == null || !currentTeam.getTeamLead().getId().equals(savedUser.getId())) {
                    currentTeam.setTeamLead(savedUser);
                    teamRepository.save(currentTeam);
                }
            }
        }
    }


    private void logActivity(User targetUser, String adminCode, String action, String description) {
        ActivityLog log = new ActivityLog();
        log.setTargetUser(targetUser);
        log.setPerformedBy(adminCode);
        log.setActionType(action);
        log.setDescription(description);

        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<TeamLeadResponseDto> getAllTeamLeadsWithDepartment() {
        List<User> teamLeads = userRepository.findAllTeamLeads();

        return teamLeads.stream().map(user -> {
            String fullName = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().isBlank()) {
                fullName += " " + user.getLastName();
            }

            return TeamLeadResponseDto.builder()
                    .employeeId(user.getId())
                    .employeeCode(user.getEmployeeCode())
                    .fullName(fullName)
                    .email(user.getEmail())
                    .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                    .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : "No Department")
                    .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                    .teamName(user.getTeam() != null ? user.getTeam().getName() : "Unassigned")
                    .build();
        }).toList();
    }
}