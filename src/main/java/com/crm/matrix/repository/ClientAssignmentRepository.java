package com.crm.matrix.repository;

import com.crm.matrix.entity.ClientAssignment;
import com.crm.matrix.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientAssignmentRepository
        extends JpaRepository<ClientAssignment, Long> {

    Page<ClientAssignment> findByEmployeeIdAndActiveTrue(
            Long employeeId,
            Pageable pageable
    );

    Optional<ClientAssignment>
    findByClientIdAndEmployeeIdAndActiveTrue(
            Long clientId,
            Long employeeId
    );

    Optional<ClientAssignment>
    findByClientIdAndActiveTrue(
            Long clientId
    );

    List<ClientAssignment>
    findByClientIdOrderByAssignedAtDesc(
            Long clientId
    );

    List<ClientAssignment>
    findByEmployeeIdAndActiveTrue(
            Long employeeId
    );

    @Query("""
        SELECT ca
        FROM ClientAssignment ca
        JOIN FETCH ca.client c
        JOIN FETCH ca.employee e
        WHERE ca.active = true
        AND c.status = com.crm.matrix.enums.ClientStatus.FOLLOW_UP
        """)
    List<ClientAssignment> findActiveFollowUps();

    @Query("""
        SELECT ca
        FROM ClientAssignment ca
        JOIN FETCH ca.client c
        JOIN FETCH ca.employee e
        WHERE ca.active = true
        AND c.status = com.crm.matrix.enums.ClientStatus.NOT_LIFTED
        """)
    List<ClientAssignment> findActiveNotLifted();

    List<ClientAssignment> findByEmployeeAndActiveTrue(User employee);

    Optional<ClientAssignment> findByIdAndEmployeeAndActiveTrue(
            Long id,
            User employee
    );

    List<ClientAssignment> findByEmployeeAndActiveTrueOrderByAssignedAtDesc(
            User employee
    );
}