package com.crm.matrix.repository;

import com.crm.matrix.entity.ClientAssignment;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    Page<ClientAssignment>
    findByEmployeeAndActiveTrueOrderByAssignedAtDesc(
            User employee,
            Pageable pageable
    );
    Optional<ClientAssignment> findByClientIdAndEmployeeAndActiveTrue(
            Long clientId,
            User employee
    );

    List<ClientAssignment>
    findByEmployeeAndActiveTrueAndClient_StatusOrderByAssignedAtDesc(
            User employee,
            ClientStatus status
    );

    Page<ClientAssignment>
    findByEmployeeAndActiveTrueAndClient_NameContainingIgnoreCase(
            User employee,
            String name,
            Pageable pageable
    );

    @Query("""
        SELECT a
        FROM ClientAssignment a
        WHERE a.active = true
        AND a.assignedAt BETWEEN :from AND :to
        ORDER BY a.assignedAt DESC
    """)
    Page<ClientAssignment> findActiveAssignmentsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    @Query("""
        SELECT a
        FROM ClientAssignment a
        WHERE a.active = true
        AND LOWER(a.client.name) LIKE LOWER(CONCAT('%', :name, '%'))
        AND a.assignedAt BETWEEN :from AND :to
        ORDER BY a.assignedAt DESC
    """)
    Page<ClientAssignment> findActiveAssignmentsByClientNameBetween(
            @Param("name") String name,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    Page<ClientAssignment>
    findByEmployeeAndActiveTrueAndClient_NameContainingIgnoreCaseAndAssignedAtBetween(
            User employee,
            String name,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
    Page<ClientAssignment>
    findByEmployeeAndActiveTrueAndAssignedAtBetween(
            User employee,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    Page<ClientAssignment>
    findByEmployeeAndActiveTrueAndClient_CurrentStageIgnoreCaseOrderByAssignedAtDesc(
            User employee,
            String currentStage,
            Pageable pageable
    );
    Page<ClientAssignment>
    findByEmployeeIdAndActiveTrueOrderByAssignedAtDesc(
            Long employeeId,
            Pageable pageable
    );
    @Query("""
        SELECT ca FROM ClientAssignment ca
        JOIN ca.client c
        WHERE ca.employee = :employee
        AND ca.active = true
        AND LOWER(c.currentStage) = 'prep'
        AND c.status IN (:statuses)
        ORDER BY ca.assignedAt DESC
    """)
    Page<ClientAssignment> findPrepAssignmentsByStatuses(
            @Param("employee") User employee,
            @Param("statuses") List<ClientStatus> statuses,
            Pageable pageable
    );

    Page<ClientAssignment> findByEmployeeIdOrderByAssignedAtDesc(
            Long employeeId,
            Pageable pageable
    );
    @Query("""
    SELECT ca FROM ClientAssignment ca 
    JOIN FETCH ca.client c 
    JOIN FETCH ca.employee e 
    WHERE ca.active = true 
    ORDER BY ca.assignedAt DESC
""")
    Page<ClientAssignment> findAllActiveAssignmentsWithDetails(Pageable pageable);

}