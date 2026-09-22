package com.crm.matrix.service;

import com.crm.matrix.dto.LoginRequest;
import com.crm.matrix.dto.LoginResponse;
import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.Role;
import com.crm.matrix.repository.PermissionRepository;
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
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String employeeCode = request.getEmployeeCode().trim();

        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("Invalid employee ID or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid employee ID or password");
        }

        String roleName = user.getRole() != null ? user.getRole().name() : null;

        // Automatically assign all active permissions to ADMIN, otherwise use user's assigned permissions
        Set<String> permissions = new HashSet<>();
        if (user.getRole() == Role.ADMIN) {
            permissions = permissionRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        } else if (user.getPermissions() != null) {
            permissions = user.getPermissions().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        }

        String accessToken = jwtService.generateToken(user, roleName, permissions);
        String refreshToken = jwtService.generateRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .employeeCode(user.getEmployeeCode())
                .role(roleName)
                .departmentName(user.getDepartment() != null ? user.getDepartment().name() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .permissions(permissions)
                .build();
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid token type. Expected a refresh token.");
        }

        String employeeCode = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User account is inactive");
        }

        String roleName = user.getRole() != null ? user.getRole().name() : null;

        Set<String> permissions = new HashSet<>();
        if (user.getRole() == Role.ADMIN) {
            permissions = permissionRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        } else if (user.getPermissions() != null) {
            permissions = user.getPermissions().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        }

        String newAccessToken = jwtService.generateToken(user, roleName, permissions);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .employeeCode(user.getEmployeeCode())
                .role(roleName)
                .departmentName(user.getDepartment() != null ? user.getDepartment().name() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .permissions(permissions)
                .build();
    }
}