package com.crm.matrix.repository;

import com.crm.matrix.entity.ClientComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientCommentRepository
        extends JpaRepository<ClientComment, Long> {

    // Get all comments for a client
    List<ClientComment>
    findByClientIdOrderByCreatedAtDesc(Long clientId);

    // Get only non-deleted comments for a client
    List<ClientComment>
    findByClientIdAndDeletedFalseOrderByCreatedAtDesc(Long clientId);

    // Get comments added by an employee
    List<ClientComment>
    findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    // Get comments for a particular assignment
    List<ClientComment>
    findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);

    // Get non-deleted comments for an employee
    List<ClientComment>
    findByEmployeeIdAndDeletedFalseOrderByCreatedAtDesc(Long employeeId);

    // Get non-deleted comments for an assignment
    List<ClientComment>
    findByAssignmentIdAndDeletedFalseOrderByCreatedAtDesc(Long assignmentId);
}
