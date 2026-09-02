package com.crm.matrix.repository;

import com.crm.matrix.entity.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendancePolicyRepository
        extends JpaRepository<AttendancePolicy, Long> {

    Optional<AttendancePolicy> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<AttendancePolicy> findByActiveTrue();

    List<AttendancePolicy> findByActiveFalse();
}