package com.crm.matrix.service;

import com.crm.matrix.dto.LoginRequest;
import com.crm.matrix.dto.LoginResponse;
import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.Role;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String employeeCode = request.getEmployeeCode().trim();


        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new RuntimeException("Invalid employee ID or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new RuntimeException("User account is inactive");
        }


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new RuntimeException("Invalid employee ID or password");
        }

        Role role = user.getRole();

        String roleName = null;

        if (role != null && Boolean.TRUE.equals(role.getActive())) {

            roleName = role.getName();
        }


        Set<String> permissions = new HashSet<>();

        if (role != null && Boolean.TRUE.equals(role.getActive()) && role.getPermissions() != null) {

            permissions = role.getPermissions().stream().filter(permission -> permission != null && Boolean.TRUE.equals(permission.getActive())).map(Permission::getCode).collect(Collectors.toSet());
        }

        String accessToken = jwtService.generateToken(user, roleName, permissions);

        String refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken).userId(user.getId()).employeeCode(user.getEmployeeCode()).role(roleName).departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null).departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null).teamId(user.getTeam() != null ? user.getTeam().getId() : null).teamName(user.getTeam() != null ? user.getTeam().getName() : null).permissions(permissions).build();
    }


    public LoginResponse refreshToken(String refreshToken) {
        // 1. Verify this is actually a refresh token, not an access token
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid token type. Expected a refresh token.");
        }

        // 2. Extract employee code and load user (validation throws exception if expired)
        String employeeCode = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User account is inactive");
        }

        // 3. Re-fetch Roles and Permissions
        Role role = user.getRole();
        String roleName = null;
        Set<String> permissions = new java.util.HashSet<>();

        if (role != null && Boolean.TRUE.equals(role.getActive())) {
            roleName = role.getName();
            if (role.getPermissions() != null) {
                permissions = role.getPermissions().stream()
                        .filter(permission -> permission != null && Boolean.TRUE.equals(permission.getActive()))
                        .map(Permission::getCode)
                        .collect(java.util.stream.Collectors.toSet());
            }
        }

        // 4. Generate new tokens
        String newAccessToken = jwtService.generateToken(user, roleName, permissions);
        String newRefreshToken = jwtService.generateRefreshToken(user); // Rotating the refresh token

        // 5. Return the new LoginResponse
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .employeeCode(user.getEmployeeCode())
                .role(roleName)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .permissions(permissions)
                .build();
    }
}