package com.crm.matrix.repository;

import com.crm.matrix.entity.ClientComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository
        extends JpaRepository<ClientComment, Long> {

    List<ClientComment> findByClientIdOrderByCreatedAtDesc(Long clientId);

   // List<ClientComment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ClientComment> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
}