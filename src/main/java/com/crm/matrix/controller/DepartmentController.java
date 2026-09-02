package com.crm.matrix.controller;

import com.crm.matrix.dto.CreateDepartmentRequest;
import com.crm.matrix.dto.DepartmentResponse;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;


    @HasPermission("DEPARTMENT_CREATE")
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {

        DepartmentResponse response = departmentService.createDepartment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @HasPermission("DEPARTMENT_READ")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {

        return ResponseEntity.ok(departmentService.getAllDepartments());
    }


    @HasPermission("DEPARTMENT_READ")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {

        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }


    @HasPermission("DEPARTMENT_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id, @Valid @RequestBody CreateDepartmentRequest request) {

        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }


    @HasPermission("DEPARTMENT_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateDepartment(@PathVariable Long id) {

        departmentService.deactivateDepartment(id);

        return ResponseEntity.noContent().build();
    }


    @HasPermission("DEPARTMENT_UPDATE")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateDepartment(@PathVariable Long id) {

        departmentService.activateDepartment(id);

        return ResponseEntity.noContent().build();
    }
}