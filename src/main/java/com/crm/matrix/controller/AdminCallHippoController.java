package com.crm.matrix.controller;

import com.crm.matrix.dto.CallHippoConfigRequest;
import com.crm.matrix.dto.CallHistoryResponse;
import com.crm.matrix.dto.ClientCallReportResponse;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.CallHippoAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminCallHippoController {

    private final UserRepository userRepository;
    private final CallHippoAdminService callHippoAdminService;


    @PutMapping("/{userId}/callhippo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> configureCallHippo(

            @PathVariable Long userId,

            @RequestBody CallHippoConfigRequest request
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        if (request.getApiToken() == null ||
                request.getApiToken().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "API token is required"
                    );
        }


        if (request.getFromNumber() == null ||
                request.getFromNumber().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "From number is required"
                    );
        }


        if (request.getAgentId() == null ||
                request.getAgentId().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Agent ID is required"
                    );
        }


        user.setCallHippoApiToken(
                request.getApiToken()
        );

        user.setCallHippoFromNumber(
                request.getFromNumber()
        );

        user.setCallHippoAgentId(
                request.getAgentId()
        );


        userRepository.save(user);


        return ResponseEntity.ok(
                "CallHippo configuration saved successfully"
        );
    }





}