package com.crm.matrix.repository;

import com.crm.matrix.entity.CallLog;
import com.crm.matrix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CallLogRepository
        extends JpaRepository<CallLog, Long> {

    List<CallLog>
    findByEmployeeOrderByStartTimeDesc(User employee);

    Optional<CallLog>
    findByIdAndEmployee(
            Long id,
            User employee
    );

    List<CallLog>
    findByClientIdOrderByStartTimeDesc(Long clientId);

    List<CallLog>
    findByAssignmentIdOrderByStartTimeDesc(Long assignmentId);

    Optional<CallLog>
    findFirstByClientIdAndEmployeeOrderByStartTimeDesc(
            Long clientId,
            User employee
    );

    Optional<CallLog>
    findByProviderCallId(String providerCallId);
}