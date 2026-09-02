package com.crm.matrix.repository;

import com.crm.matrix.entity.CallRecord;
import com.crm.matrix.enums.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallRecordRepository
        extends JpaRepository<CallRecord, Long> {

    Page<CallRecord> findAllByOrderByStartedAtDesc(
            Pageable pageable
    );

    Page<CallRecord> findByEmployeeId(
            Long employeeId,
            Pageable pageable
    );

    Page<CallRecord> findByClientId(
            Long clientId,
            Pageable pageable
    );

    List<CallRecord> findByEmployeeIdAndStartedAtBetween(
            Long employeeId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<CallRecord> findByStartedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByEmployeeId(Long employeeId);

    long countByEmployeeIdAndCallStatus(
            Long employeeId,
            CallStatus status
    );

    long countByCallStatus(
            CallStatus status
    );
}