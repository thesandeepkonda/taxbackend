//package com.crm.matrix.service;
//
//import com.crm.matrix.dto.CreateDepartmentRequest;
//import com.crm.matrix.dto.DepartmentResponse;
//import com.crm.matrix.entity.Department;
//import com.crm.matrix.repository.DepartmentRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class DepartmentService {
//
//    private final DepartmentRepository departmentRepository;
//
//
//    @Transactional
//    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
//
//        String name = request.getName().trim();
//
//
//        if (departmentRepository.existsByNameIgnoreCase(name)) {
//
//            throw new IllegalArgumentException("Department already exists: " + name);
//        }
//
//        Department department = new Department();
//
//        department.setName(name);
//
//        if (request.getDescription() != null && !request.getDescription().isBlank()) {
//
//            department.setDescription(request.getDescription().trim());
//        }
//
//        department.setActive(true);
//
//        Department saved = departmentRepository.save(department);
//
//        return mapToResponse(saved);
//    }
//
//
//    @Transactional(readOnly = true)
//    public List<DepartmentResponse> getAllDepartments() {
//
//        return departmentRepository.findAll().stream().map(this::mapToResponse).toList();
//    }
//
//
//    @Transactional(readOnly = true)
//    public DepartmentResponse getDepartmentById(Long id) {
//
//        Department department = departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
//
//        return mapToResponse(department);
//    }
//
//
//    @Transactional
//    public DepartmentResponse updateDepartment(Long id, CreateDepartmentRequest request) {
//
//        Department department = departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
//
//        String name = request.getName().trim();
//
//
//        if (!department.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
//
//            throw new IllegalArgumentException("Department already exists: " + name);
//        }
//
//
//        department.setName(name);
//
//
//        if (request.getDescription() != null && !request.getDescription().isBlank()) {
//
//            department.setDescription(request.getDescription().trim());
//
//        } else {
//
//            department.setDescription(null);
//        }
//
//        Department updated = departmentRepository.save(department);
//
//        return mapToResponse(updated);
//    }
//
//
//    @Transactional
//    public void deactivateDepartment(Long id) {
//
//        Department department = departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
//
//        department.setActive(false);
//
//        departmentRepository.save(department);
//    }
//
//
//    @Transactional
//    public void activateDepartment(Long id) {
//
//        Department department = departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
//
//        department.setActive(true);
//
//        departmentRepository.save(department);
//    }
//
//
//    private DepartmentResponse mapToResponse(Department department) {
//
//        return DepartmentResponse.builder().id(department.getId()).name(department.getName()).description(department.getDescription()).active(department.getActive()).build();
//    }
//
//
//    @Transactional(readOnly = true)
//    public Page<DepartmentResponse> getDepartmentsByStatus(boolean active, Pageable pageable) {
//        return departmentRepository.findByActive(active, pageable)
//                .map(this::mapToResponse);
//    }
//}