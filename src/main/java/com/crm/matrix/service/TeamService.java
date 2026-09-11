package com.crm.matrix.service;

import com.crm.matrix.dto.CreateEmployeeResponse;
import com.crm.matrix.dto.CreateTeamRequest;
import com.crm.matrix.dto.TeamResponse;
import com.crm.matrix.entity.Department;
import com.crm.matrix.entity.Role;
import com.crm.matrix.entity.Team;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.DepartmentRepository;
import com.crm.matrix.repository.RoleRepository;
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

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {

        String name = request.getName().trim();

        Long departmentId = request.getDepartmentId();

        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));


        if (!Boolean.TRUE.equals(department.getActive())) {

            throw new IllegalArgumentException("Cannot create team under an inactive department");
        }


        if (teamRepository.existsByNameIgnoreCaseAndDepartmentId(name, departmentId)) {

            throw new IllegalArgumentException("Team already exists in this department: " + name);
        }


        Team team = new Team();

        team.setName(name);

        team.setDepartment(department);

        // Team lead will be assigned after employees exist
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
    public List<TeamResponse> getTeamsByDepartment(Long departmentId) {

        departmentRepository.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        return teamRepository.findByDepartmentId(departmentId).stream().map(this::mapToResponse).toList();
    }


    @Transactional
    public TeamResponse updateTeam(Long id, CreateTeamRequest request) {

        Team team = teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));

        String name = request.getName().trim();

        Long departmentId = request.getDepartmentId();

        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        if (!Boolean.TRUE.equals(department.getActive())) {

            throw new IllegalArgumentException("Cannot assign team to an inactive department");
        }

        // Check duplicate only when name or department changes
        boolean departmentChanged = !team.getDepartment().getId().equals(departmentId);

        boolean nameChanged = !team.getName().equalsIgnoreCase(name);

        if ((nameChanged || departmentChanged) && teamRepository.existsByNameIgnoreCaseAndDepartmentId(name, departmentId)) {

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

        if (!Boolean.TRUE.equals(team.getDepartment().getActive())) {

            throw new IllegalArgumentException("Cannot activate team because its department is inactive");
        }

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

        return TeamResponse.builder().teamId(team.getId()).name(team.getName()).departmentId(team.getDepartment().getId()).departmentName(team.getDepartment().getName()).teamLeadId(teamLeadId).teamLeadName(teamLeadName).active(team.getActive()).build();
    }

    @Transactional(readOnly = true)
    public List<CreateEmployeeResponse> getUsersByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        List<User> users = userRepository.findByTeamIdAndActiveTrue(teamId);
        return users.stream().map(user -> CreateEmployeeResponse.builder().employeeCode(user.getEmployeeCode()).id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName()).email(user.getEmail()).phone(user.getPhone()).departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null).departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null).teamId(user.getTeam() != null ? user.getTeam().getId() : null).teamName(user.getTeam() != null ? user.getTeam().getName() : null).roleId(user.getRole() != null ? user.getRole().getId() : null).roleName(user.getRole() != null ? user.getRole().getName() : null).active(user.getActive()).build()).toList();
    }

    @Transactional
    public TeamResponse assignTeamLead(Long teamId, Long employeeId, boolean override) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        User newLead = userRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        if (newLead.getDepartment() == null || !newLead.getDepartment().getId().equals(team.getDepartment().getId())) {
            throw new IllegalArgumentException("Team lead must belong to the team's department");
        }

        Role employeeRole = roleRepository.findByNameIgnoreCase("EMPLOYEE").orElseThrow(() -> new IllegalStateException("EMPLOYEE role not found in the database"));

        Role teamLeadRole = roleRepository.findByNameIgnoreCase("TEAM_LEAD").orElseThrow(() -> new IllegalStateException("TEAM_LEAD role not found in the database"));

        if (team.getTeamLead() != null && !team.getTeamLead().getId().equals(newLead.getId())) {
            if (!override) {
                throw new IllegalStateException("Team already has a team lead assigned. Please confirm override to replace them.");
            } else {
                User oldLead = team.getTeamLead();
                oldLead.setRole(employeeRole);
                userRepository.save(oldLead);
                team.setTeamLead(null);
                teamRepository.saveAndFlush(team);
            }
        }

        newLead.setRole(teamLeadRole);
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

        // Identify the Team Lead's ID so we can filter them out
        Long teamLeadId = (loggedInUser.getTeam().getTeamLead() != null)
                ? loggedInUser.getTeam().getTeamLead().getId()
                : null;

        // Fetch all team members, then filter out the Team Lead
        return getUsersByTeam(loggedInUser.getTeam().getId()).stream()
                .filter(employee -> !employee.getId().equals(teamLeadId))
                .toList();
    }
}