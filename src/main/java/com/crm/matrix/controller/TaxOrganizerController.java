package com.crm.matrix.controller;

import com.crm.matrix.dto.TaxOrganizerRequest;
import com.crm.matrix.dto.TaxOrganizerResponse;
import com.crm.matrix.service.TaxOrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
public class TaxOrganizerController {

    private final TaxOrganizerService taxOrganizerService;

    @GetMapping("/clients/{clientId}/tax-organizer")
    public ResponseEntity<TaxOrganizerResponse> getOrganizer(
            @PathVariable Long clientId,
            Authentication authentication) {

        return ResponseEntity.ok(
                taxOrganizerService.getOrganizer(
                        clientId,
                        authentication
                )
        );
    }

    @PostMapping("/clients/{clientId}/tax-organizer")
    public ResponseEntity<TaxOrganizerResponse> createOrganizer(
            @PathVariable Long clientId,
            @RequestBody TaxOrganizerRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                taxOrganizerService.saveOrganizer(
                        clientId,
                        request,
                        authentication
                )
        );
    }

    @PutMapping("/clients/{clientId}/tax-organizer")
    public ResponseEntity<TaxOrganizerResponse> updateOrganizer(
            @PathVariable Long clientId,
            @RequestBody TaxOrganizerRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                taxOrganizerService.saveOrganizer(
                        clientId,
                        request,
                        authentication
                )
        );
    }

    @PostMapping("/clients/{clientId}/tax-organizer/submit")
    public ResponseEntity<TaxOrganizerResponse> submitOrganizer(
            @PathVariable Long clientId,
            Authentication authentication) {

        return ResponseEntity.ok(
                taxOrganizerService.submitOrganizer(
                        clientId,
                        authentication
                )
        );
    }

    @GetMapping("/clients/{clientId}/tax-organizer/view")
    public ResponseEntity<Resource> viewOrganizer(
            @PathVariable Long clientId,
            Authentication authentication) {

        return taxOrganizerService.viewOrganizer(
                clientId,
                authentication
        );
    }

    @GetMapping("/clients/{clientId}/tax-organizer/download")
    public ResponseEntity<Resource> downloadOrganizer(
            @PathVariable Long clientId,
            Authentication authentication) {

        return taxOrganizerService.downloadOrganizer(
                clientId,
                authentication
        );
    }
}