package com.crm.matrix.controller;


import com.crm.matrix.dto.CreateEmployeeResponse;
import com.crm.matrix.dto.CreateTeamRequest;
import com.crm.matrix.dto.TeamLeadResponseDto;
import com.crm.matrix.dto.TeamResponse;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;


    @HasPermission("TEAM_CREATE")
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {

        TeamResponse response = teamService.createTeam(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @HasPermission("TEAM_READ")
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {

        return ResponseEntity.ok(teamService.getAllTeams());
    }


    @HasPermission("TEAM_READ")
    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {

        return ResponseEntity.ok(teamService.getTeamById(id));
    }


    @HasPermission("TEAM_READ")
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByDepartment(@PathVariable Long departmentId) {

        return ResponseEntity.ok(teamService.getTeamsByDepartment(departmentId));
    }

    @HasPermission("TEAM_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {

        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    @HasPermission("TEAM_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateTeam(@PathVariable Long id) {

        teamService.deactivateTeam(id);

        return ResponseEntity.noContent().build();
    }


    @HasPermission("TEAM_UPDATE")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateTeam(@PathVariable Long id) {

        teamService.activateTeam(id);

        return ResponseEntity.noContent().build();
    }

    @HasPermission("TEAM_READ")
    @GetMapping("/{teamId}/users")
    public ResponseEntity<List<CreateEmployeeResponse>> getUsersByTeam(@PathVariable Long teamId) {

        return ResponseEntity.ok(teamService.getUsersByTeam(teamId));
    }
    @GetMapping("/my-team/users")
    public ResponseEntity<List<CreateEmployeeResponse>> getMyTeamUsers(Authentication authentication) {
        return ResponseEntity.ok(teamService.getMyTeamUsers(authentication));
    }



    @HasPermission("TEAM_UPDATE")
    @PatchMapping("/{teamId}/assign-lead/{employeeId}")
    public ResponseEntity<TeamResponse> assignTeamLead(
            @PathVariable Long teamId,
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "false") boolean override) { // Added override parameter

        return ResponseEntity.ok(teamService.assignTeamLead(teamId, employeeId, override));
    }


    @HasPermission("TEAM_READ")
    @GetMapping("/status")
    public ResponseEntity<Page<TeamResponse>> getTeamsByStatus(
            @RequestParam boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(teamService.getTeamsByStatus(active, pageable));
    }
}