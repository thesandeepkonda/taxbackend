package com.crm.matrix.controller;

import com.crm.matrix.dto.BulkEmployeeResponse;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.BulkEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class BulkEmployeeController {

    private final BulkEmployeeService bulkEmployeeService;

    @HasPermission("USER_CREATE")
    @PostMapping("/bulk-upload")
    public ResponseEntity<BulkEmployeeResponse> uploadEmployees(
            @RequestParam("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                bulkEmployeeService.uploadEmployees(file)
        );
    }
}