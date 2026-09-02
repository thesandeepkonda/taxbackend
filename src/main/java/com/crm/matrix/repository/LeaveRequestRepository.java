package com.crm.matrix.repository;

import com.crm.matrix.entity.LeaveRequest;
import com.crm.matrix.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
    );}