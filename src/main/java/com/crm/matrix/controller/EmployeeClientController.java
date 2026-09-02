package com.crm.matrix.controller;


import com.crm.matrix.dto.AssignedClientResponseDto;
import com.crm.matrix.service.EmployeeClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/clients")
@RequiredArgsConstructor
public class EmployeeClientController {

    private final EmployeeClientService employeeClientService;


    // =========================================================
    // GET MY ASSIGNED CLIENTS
    // =========================================================

    @GetMapping
    public ResponseEntity<Page<AssignedClientResponseDto>>
    getMyClients(
            Authentication authentication,
            Pageable pageable) {

        return ResponseEntity.ok(
                employeeClientService.getMyClients(
                        authentication,
                        pageable
                )
        );
    }


    // =========================================================
    // GET MY ASSIGNED CLIENT
    // =========================================================

    @GetMapping("/{clientId}")
    public ResponseEntity<AssignedClientResponseDto>
    getMyClient(
            @PathVariable Long clientId,
            Authentication authentication) {

        return ResponseEntity.ok(
                employeeClientService.getMyClient(
                        clientId,
                        authentication
                )
        );
    }
}