package com.crm.matrix.repository;

import com.crm.matrix.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Automatically fetches the history for an employee, newest first
    List<ActivityLog> findByTargetUserIdOrderByCreatedAtDesc(Long userId);
}