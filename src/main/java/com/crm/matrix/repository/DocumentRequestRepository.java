package com.crm.matrix.repository;

import com.crm.matrix.entity.DocumentRequest;
import com.crm.matrix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRequestRepository
        extends JpaRepository<DocumentRequest, Long> {

    Optional<DocumentRequest> findByShareTokenAndActiveTrue(
            String shareToken
    );

    List<DocumentRequest>
    findByEmployeeAndActiveTrueOrderByCreatedAtDesc(
            User employee
    );

    Optional<DocumentRequest> findByIdAndEmployee(
            Long id,
            User employee
    );
}