package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.Role;
import com.crm.matrix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailService emailService;

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

        Department department = request.getDepartment();
        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }

        AttendancePolicy attendancePolicy = attendancePolicyRepository.findById(request.getAttendancePolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + request.getAttendancePolicyId()));
        if (!Boolean.TRUE.equals(attendancePolicy.getActive())) {
            throw new IllegalArgumentException("Cannot assign an inactive attendance policy");
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.getTeamId()));

            if (!Boolean.TRUE.equals(team.getActive())) {
                throw new IllegalArgumentException("Cannot assign an inactive team");
            }

            if (team.getDepartment() == null) {
                throw new IllegalArgumentException("Team is not assigned to a department");
            }

            // Compare Enum to Enum
            if (team.getDepartment() != department) {
                throw new IllegalArgumentException("Selected team does not belong to selected department");
            }
        }

        Role role = request.getRole();

        if (role == Role.TEAM_LEAD) {
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

        if (role == Role.TEAM_LEAD) {
            team.setTeamLead(savedUser);
            teamRepository.save(team);
        }
        emailService.sendCredentialsEmail(savedUser.getEmail(), savedUser.getEmployeeCode(), temporaryPassword);

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

        Department department = request.getDepartment();
        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }

        AttendancePolicy attendancePolicy = attendancePolicyRepository.findById(request.getAttendancePolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + request.getAttendancePolicyId()));

        if (!Boolean.TRUE.equals(attendancePolicy.getActive())) {
            throw new IllegalArgumentException("Cannot assign an inactive attendance policy");
        }

        Team newTeam = null;
        if (request.getTeamId() != null) {
            newTeam = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.getTeamId()));

            if (!Boolean.TRUE.equals(newTeam.getActive())) {
                throw new IllegalArgumentException("Cannot assign an inactive team");
            }

            if (newTeam.getDepartment() == null || newTeam.getDepartment() != department) {
                throw new IllegalArgumentException("Selected team does not belong to the selected department");
            }
        }

        Role newRole = request.getRole();
        Team oldTeam = user.getTeam();
        Role oldRole = user.getRole();
        Department oldDepartment = user.getDepartment();

        if (newRole == Role.TEAM_LEAD) {
            if (newTeam == null) {
                throw new IllegalArgumentException("A TEAM_LEAD must be assigned to a team");
            }
            if (newTeam.getTeamLead() != null && !newTeam.getTeamLead().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Team already has a team lead: " + newTeam.getTeamLead().getEmployeeCode());
            }
        }

        if (oldRole == Role.TEAM_LEAD && (newRole != Role.TEAM_LEAD || (oldTeam != null && newTeam != null && !oldTeam.getId().equals(newTeam.getId())))) {
            List<Team> teamsLed = teamRepository.findByTeamLeadId(user.getId());
            for (Team team : teamsLed) {
                team.setTeamLead(null);
                teamRepository.save(team);
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

        if (newRole == Role.TEAM_LEAD) {
            newTeam.setTeamLead(savedUser);
            teamRepository.save(newTeam);
        }

        // =========================================================
        // AUDIT LOGGING FOR FULL EDIT ENDPOINT
        // =========================================================

        if (oldDepartment != department) {
            String oldDeptName = oldDepartment != null ? oldDepartment.name() : "None";
            logActivity(savedUser, adminEmployeeCode, "DEPARTMENT_CHANGED",
                    "Moved from department " + oldDeptName + " to " + department.name());
        }

        if (request.getTeamId() != null && (oldTeam == null || !oldTeam.getId().equals(newTeam.getId()))) {
            String oldName = oldTeam != null ? oldTeam.getName() : "None";
            logActivity(savedUser, adminEmployeeCode, "TEAM_CHANGED",
                    "Moved from team " + oldName + " to " + newTeam.getName());
        } else if (request.getTeamId() == null && oldTeam != null) {
            logActivity(savedUser, adminEmployeeCode, "TEAM_CHANGED",
                    "Removed from team " + oldTeam.getName());
        }

        if (oldRole != newRole) {
            String oldRoleName = oldRole != null ? oldRole.name() : "None";
            logActivity(savedUser, adminEmployeeCode, "ROLE_CHANGED",
                    "Changed role from " + oldRoleName + " to " + (newRole != null ? newRole.name() : "None"));
        }

        return buildEmployeeResponse(savedUser, null);
    }

    @Transactional(readOnly = true)
    public List<CreateEmployeeResponse> getAllEmployees() {
        return userRepository.findAll().stream().map(user -> buildEmployeeResponse(user, null)).toList();
    }

    private CreateEmployeeResponse buildEmployeeResponse(User user, String temporaryPassword) {
        return CreateEmployeeResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())

                // Assuming CreateEmployeeResponse is refactored to take Enums directly or String representations
                .departmentName(user.getDepartment() != null ? user.getDepartment().name() : null)

                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)

                .roleName(user.getRole() != null ? user.getRole().name() : null)

                .attendancePolicyId(user.getAttendancePolicy() != null ? user.getAttendancePolicy().getId() : null)
                .attendancePolicyName(user.getAttendancePolicy() != null ? user.getAttendancePolicy().getName() : null)
                .workMode(user.getWorkMode() != null ? user.getWorkMode().name() : null)
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
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + userId));

        return activityLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(log -> ActivityLogResponseDto.builder()
                        .id(log.getId())
                        .performedBy(log.getPerformedBy())
                        .actionType(log.getActionType())
                        .description(log.getDescription())
                        .timestamp(log.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void quickAssign(Long userId, QuickAssignRequestDto request, String adminEmployeeCode, boolean override) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + userId));

        Department oldDepartment = user.getDepartment();
        Team oldTeam = user.getTeam();
        Role oldRole = user.getRole();
        boolean isModified = false;

        // =========================================================
        // 1. UPDATE DEPARTMENT
        // =========================================================
        if (request.getDepartment() != null && oldDepartment != request.getDepartment()) {
            Department newDepartment = request.getDepartment();
            user.setDepartment(newDepartment);
            logActivity(user, adminEmployeeCode, "DEPARTMENT_CHANGED",
                    "Moved from " + (oldDepartment != null ? oldDepartment.name() : "None") +
                            " to " + newDepartment.name());
            isModified = true;
        }

        // =========================================================
        // 2. UPDATE TEAM & TEAM LEAD VALIDATION
        // =========================================================
        if (request.getTeamId() != null &&
                (oldTeam == null || !oldTeam.getId().equals(request.getTeamId()))) {
            Team newTeam = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));

            if (!Boolean.TRUE.equals(newTeam.getActive())) {
                throw new IllegalArgumentException("Cannot assign to an inactive team");
            }

            if (newTeam.getDepartment() != user.getDepartment()) {
                throw new IllegalArgumentException("Team does not belong to the employee's department");
            }

            Role targetRole = request.getRole() != null ? request.getRole() : user.getRole();

            if (targetRole == Role.TEAM_LEAD) {
                if (newTeam.getTeamLead() != null && !newTeam.getTeamLead().getId().equals(user.getId())) {
                    if (!override) {
                        throw new IllegalStateException("Team already has a team lead assigned. Please confirm override to replace them.");
                    } else {
                        User oldLead = newTeam.getTeamLead();
                        oldLead.setRole(Role.EMPLOYEE);
                        userRepository.save(oldLead);
                    }
                }
            }

            // If user was a TEAM_LEAD of their previous team, nullify it
            if (oldRole == Role.TEAM_LEAD && oldTeam != null && oldTeam.getTeamLead() != null && oldTeam.getTeamLead().getId().equals(user.getId())) {
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
        // 3. UPDATE ROLE & CLEAR TEAM LEAD POINTER IF DEMOTED
        // =========================================================
        if (request.getRole() != null && oldRole != request.getRole()) {
            Role newRole = request.getRole();
            Team targetTeam = user.getTeam();

            if (newRole == Role.TEAM_LEAD) {
                if (targetTeam == null) {
                    throw new IllegalArgumentException("A TEAM_LEAD must be assigned to a team");
                }
                if (targetTeam.getTeamLead() != null && !targetTeam.getTeamLead().getId().equals(user.getId())) {
                    if (!override) {
                        throw new IllegalStateException("Team already has a team lead assigned. Please confirm override to replace them.");
                    } else {
                        User oldLead = targetTeam.getTeamLead();
                        oldLead.setRole(Role.EMPLOYEE);
                        userRepository.save(oldLead);
                    }
                }
            } else if (oldRole == Role.TEAM_LEAD) {
                // ---> FIX: When demoted from TEAM_LEAD to EMPLOYEE/ADMIN, detach them from any team they lead <---
                List<Team> teamsLed = teamRepository.findByTeamLeadId(user.getId());
                for (Team team : teamsLed) {
                    team.setTeamLead(null);
                    teamRepository.save(team);
                }
            }

            user.setRole(newRole);
            logActivity(user, adminEmployeeCode, "ROLE_CHANGED",
                    "Changed role from " + (oldRole != null ? oldRole.name() : "None") +
                            " to " + newRole.name());
            isModified = true;
        }

        // =========================================================
        // 4. SAVE CHANGES & TEAM LEAD LOGIC
        // =========================================================
        if (isModified) {
            User savedUser = userRepository.save(user);
            if (savedUser.getRole() == Role.TEAM_LEAD && savedUser.getTeam() != null) {
                Team currentTeam = savedUser.getTeam();
                currentTeam.setTeamLead(savedUser);
                teamRepository.save(currentTeam);
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
                    .departmentName(user.getDepartment() != null ? user.getDepartment().name() : "No Department")
                    .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                    .teamName(user.getTeam() != null ? user.getTeam().getName() : "Unassigned")
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public Page<CreateEmployeeResponse> getUsersByStatus(boolean active, Pageable pageable) {
        return userRepository.findByActive(active, pageable)
                .map(user -> buildEmployeeResponse(user, null));
    }

    @Transactional
    public void updateUserStatus(Long id, boolean active, String adminEmployeeCode) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        if (Boolean.TRUE.equals(user.getActive()) == active) {
            throw new IllegalStateException("User is already " + (active ? "active" : "inactive"));
        }

        user.setActive(active);
        userRepository.save(user);

        String action = active ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        String description = active ? "Activated user account" : "Deactivated user account";
        logActivity(user, adminEmployeeCode, action, description);
    }

    @Transactional
    public String adminResetPassword(Long userId, String adminEmployeeCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + userId));

        String newTemporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(newTemporaryPassword));
        userRepository.save(user);
        logActivity(user, adminEmployeeCode, "PASSWORD_RESET", "Admin performed password reset");

        return newTemporaryPassword;
    }

    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Account is inactive");
        }

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
    }

    @Transactional
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp.trim())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
    }
}