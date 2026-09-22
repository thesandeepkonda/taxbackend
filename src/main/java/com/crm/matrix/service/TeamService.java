package com.crm.matrix.service;

import com.crm.matrix.dto.CreateEmployeeResponse;
import com.crm.matrix.dto.CreateTeamRequest;
import com.crm.matrix.dto.TeamResponse;
import com.crm.matrix.entity.Team;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.Role;
import com.crm.matrix.repository.TeamRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {

        String name = request.getName().trim();
        Department department = request.getDepartment();

        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }

        // Note: Update TeamRepository to use the Department enum instead of departmentId
        if (teamRepository.existsByNameIgnoreCaseAndDepartment(name, department)) {
            throw new IllegalArgumentException("Team already exists in this department: " + name);
        }

        Team team = new Team();
        team.setName(name);
        team.setDepartment(department);
        team.setTeamLead(null);
        team.setActive(true);

        Team savedTeam = teamRepository.save(team);

        return mapToResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));
        return mapToResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }
        // Note: Update TeamRepository to use the Department enum
        return teamRepository.findByDepartment(department).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TeamResponse updateTeam(Long id, CreateTeamRequest request) {

        Team team = teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));

        String name = request.getName().trim();
        Department department = request.getDepartment();

        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }

        boolean departmentChanged = team.getDepartment() != department;
        boolean nameChanged = !team.getName().equalsIgnoreCase(name);

        if ((nameChanged || departmentChanged) && teamRepository.existsByNameIgnoreCaseAndDepartment(name, department)) {
            throw new IllegalArgumentException("Team already exists in this department: " + name);
        }

        team.setName(name);
        team.setDepartment(department);

        Team updatedTeam = teamRepository.save(team);

        return mapToResponse(updatedTeam);
    }

    @Transactional
    public void deactivateTeam(Long id) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));
        team.setActive(false);
        teamRepository.save(team);
    }

    @Transactional
    public void activateTeam(Long id) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));
        team.setActive(true);
        teamRepository.save(team);
    }

    private TeamResponse mapToResponse(Team team) {
        Long teamLeadId = null;
        String teamLeadName = null;

        if (team.getTeamLead() != null) {
            teamLeadId = team.getTeamLead().getId();
            teamLeadName = team.getTeamLead().getFirstName();

            if (team.getTeamLead().getLastName() != null && !team.getTeamLead().getLastName().isBlank()) {
                teamLeadName = teamLeadName + " " + team.getTeamLead().getLastName();
            }
        }

        return TeamResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .department(team.getDepartment())
                .teamLeadId(teamLeadId)
                .teamLeadName(teamLeadName)
                .active(team.getActive())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CreateEmployeeResponse> getUsersByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        List<User> users = userRepository.findByTeamIdAndActiveTrue(teamId);

        return users.stream().map(user -> CreateEmployeeResponse.builder()
                .employeeCode(user.getEmployeeCode())
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentName(user.getDepartment() != null ? user.getDepartment().name() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .roleName(user.getRole() != null ? user.getRole().name() : null)
                .active(user.getActive())
                .build()).toList();
    }

    @Transactional
    public TeamResponse assignTeamLead(Long teamId, Long employeeId, boolean override) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
        User newLead = userRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        if (newLead.getDepartment() == null || newLead.getDepartment() != team.getDepartment()) {
            throw new IllegalArgumentException("Team lead must belong to the team's department");
        }

        if (team.getTeamLead() != null && !team.getTeamLead().getId().equals(newLead.getId())) {
            if (!override) {
                throw new IllegalStateException("Team already has a team lead assigned. Please confirm override to replace them.");
            } else {
                User oldLead = team.getTeamLead();
                oldLead.setRole(Role.EMPLOYEE);
                userRepository.save(oldLead);
                team.setTeamLead(null);
                teamRepository.saveAndFlush(team);
            }
        }

        newLead.setRole(Role.TEAM_LEAD);
        newLead.setTeam(team);
        userRepository.save(newLead);

        team.setTeamLead(newLead);
        Team savedTeam = teamRepository.save(team);

        return mapToResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public Page<TeamResponse> getTeamsByStatus(boolean active, Pageable pageable) {
        return teamRepository.findByActive(active, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CreateEmployeeResponse> getMyTeamUsers(Authentication authentication) {
        User loggedInUser = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));

        if (loggedInUser.getTeam() == null) {
            throw new RuntimeException("You are not assigned to any team");
        }

        Long teamLeadId = (loggedInUser.getTeam().getTeamLead() != null)
                ? loggedInUser.getTeam().getTeamLead().getId()
                : null;

        return getUsersByTeam(loggedInUser.getTeam().getId()).stream()
                .filter(employee -> !employee.getId().equals(teamLeadId))
                .toList();
    }
}