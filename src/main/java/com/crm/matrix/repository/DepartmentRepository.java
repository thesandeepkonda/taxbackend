//package com.crm.matrix.repository;
//
//import aj.org.objectweb.asm.commons.Remapper;
//import com.crm.matrix.entity.Department;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface DepartmentRepository extends JpaRepository<Department, Long> {
//
//    Optional<Department> findByNameIgnoreCase(String name);
//
//    boolean existsByNameIgnoreCase(String name);
//
//    Page<Department> findByActive(boolean active, Pageable pageable);
//}