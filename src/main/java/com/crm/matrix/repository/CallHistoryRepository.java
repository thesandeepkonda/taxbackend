package com.crm.matrix.repository;

import com.crm.matrix.entity.CallHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallHistoryRepository
        extends JpaRepository<CallHistory, Long> {

    Optional<CallHistory> findByCallSid(String callSid);

    boolean existsByCallSid(String callSid);

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByClientId(
            Long clientId,
            Pageable pageable
    );

    Optional<CallHistory>
    findTopByClientIdAndUserIdAndToNumberOrderByCallTimeDesc(
            Long clientId,
            Long userId,
            String toNumber
    );

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserId(
            Long userId,
            Pageable pageable
    );
}