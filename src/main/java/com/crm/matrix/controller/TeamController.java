package com.crm.matrix.controller;

import com.crm.matrix.dto.CreateEmployeeResponse;
import com.crm.matrix.dto.CreateTeamRequest;
import com.crm.matrix.dto.TeamLeadResponseDto;
import com.crm.matrix.dto.TeamResponse;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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



    @HasPermission("TEAM_UPDATE")
    @PatchMapping("/teams/{teamId}/assign-lead/{employeeId}")
    public ResponseEntity<TeamResponse> assignTeamLead(
            @PathVariable Long teamId,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(teamService.assignTeamLead(teamId, employeeId));
    }
}