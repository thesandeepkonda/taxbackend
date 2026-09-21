package com.crm.matrix.repository;

import com.crm.matrix.entity.CallHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallHistoryRepository
        extends JpaRepository<CallHistory, Long> {


    // =========================================================
    // FIND BY CALL SID
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Optional<CallHistory> findByCallSid(String callSid);


    boolean existsByCallSid(String callSid);


    // =========================================================
    // ALL CALLS
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findAllBy(
            Pageable pageable
    );


    // =========================================================
    // ALL CALLS WITH DATE FILTER
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByCallTimeBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    // =========================================================
    // USER CALLS
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserId(
            Long userId,
            Pageable pageable
    );


    // =========================================================
    // USER CALLS WITH DATE
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserIdAndCallTimeBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    // =========================================================
    // MULTIPLE USERS
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserIdIn(
            List<Long> userIds,
            Pageable pageable
    );


    // =========================================================
    // MULTIPLE USERS WITH DATE
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserIdInAndCallTimeBetween(
            List<Long> userIds,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    // =========================================================
    // CLIENT CALL HISTORY
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByClientId(
            Long clientId,
            Pageable pageable
    );


    // =========================================================
    // CLIENT CALL HISTORY WITH DATE
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByClientIdAndCallTimeBetween(
            Long clientId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    // =========================================================
    // EMPLOYEE + CLIENT
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserIdAndClientId(
            Long userId,
            Long clientId,
            Pageable pageable
    );


    // =========================================================
    // EMPLOYEE + CLIENT + DATE
    // =========================================================

    @EntityGraph(attributePaths = {"client", "user"})
    Page<CallHistory> findByUserIdAndClientIdAndCallTimeBetween(
            Long userId,
            Long clientId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    // =========================================================
    // LAST CALL FOR CLIENT
    // =========================================================

    Optional<CallHistory>
    findTopByClientIdAndUserIdAndToNumberOrderByCallTimeDesc(
            Long clientId,
            Long userId,
            String toNumber
    );


    // =========================================================
    // USER STATISTICS
    // =========================================================

    long countByUserId(Long userId);


    long countByUserIdAndStatus(
            Long userId,
            String status
    );


    @Query("""
        SELECT COALESCE(SUM(c.durationSeconds), 0)
        FROM CallHistory c
        WHERE c.user.id = :userId
    """)
    Long getTotalTalkTimeByUserId(
            @Param("userId") Long userId
    );


    // =========================================================
    // TEAM STATISTICS
    // =========================================================

    @Query("""
        SELECT COUNT(c)
        FROM CallHistory c
        WHERE c.user.id IN :userIds
    """)
    long countByUserIds(
            @Param("userIds") List<Long> userIds
    );


    @Query("""
        SELECT COUNT(c)
        FROM CallHistory c
        WHERE c.user.id IN :userIds
        AND UPPER(c.status) = UPPER(:status)
    """)
    long countByUserIdsAndStatus(
            @Param("userIds") List<Long> userIds,
            @Param("status") String status
    );


    @Query("""
        SELECT COALESCE(SUM(c.durationSeconds), 0)
        FROM CallHistory c
        WHERE c.user.id IN :userIds
    """)
    Long getTotalTalkTimeByUserIds(
            @Param("userIds") List<Long> userIds
    );


    // =========================================================
    // ALL CALL STATISTICS
    // =========================================================

    @Query("""
        SELECT COUNT(c)
        FROM CallHistory c
    """)
    long countAllCalls();


    @Query("""
        SELECT COUNT(c)
        FROM CallHistory c
        WHERE UPPER(c.status) = UPPER(:status)
    """)
    long countAllCallsByStatus(
            @Param("status") String status
    );


    @Query("""
        SELECT COALESCE(SUM(c.durationSeconds), 0)
        FROM CallHistory c
    """)
    Long getTotalTalkTime();


    // =========================================================
    // CLIENT STATISTICS
    // =========================================================

    long countByClientId(Long clientId);


    long countByClientIdAndStatus(
            Long clientId,
            String status
    );


    @Query("""
        SELECT COALESCE(SUM(c.durationSeconds), 0)
        FROM CallHistory c
        WHERE c.client.id = :clientId
    """)
    Long getTotalTalkTimeByClientId(
            @Param("clientId") Long clientId
    );
}