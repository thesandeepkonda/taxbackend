package com.crm.matrix.repository;

import com.crm.matrix.entity.LeaveRequest;
import com.crm.matrix.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest>
    findByUserIdOrderByFromDateDesc(
            Long userId
    );


    List<LeaveRequest>
    findByStatusOrderByFromDateAsc(
            LeaveRequestStatus status
    );


    List<LeaveRequest>
    findByUserIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long userId,
            LocalDate date1,
            LocalDate date2
    );


    boolean existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long userId,
            LeaveRequestStatus status,
            LocalDate date1,
            LocalDate date2
    );

    List<LeaveRequest> findByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long userId,
            LeaveRequestStatus status,
            LocalDate date1,
            LocalDate date2
    );

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = :status AND :today BETWEEN lr.fromDate AND lr.toDate")
    List<LeaveRequest> findActiveLeaves(@Param("status") LeaveRequestStatus status, @Param("today") LocalDate today);

    // 2. FOR TEAM LEAD: Get currently active leaves for a specific team
    @Query("SELECT lr FROM LeaveRequest lr JOIN lr.user u WHERE u.team.id = :teamId AND lr.status = :status AND :today BETWEEN lr.fromDate AND lr.toDate")
    List<LeaveRequest> findActiveLeavesByTeam(@Param("teamId") Long teamId, @Param("status") LeaveRequestStatus status, @Param("today") LocalDate today);
}