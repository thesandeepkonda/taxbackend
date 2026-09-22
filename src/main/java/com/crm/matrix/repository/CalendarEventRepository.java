package com.crm.matrix.repository;

import com.crm.matrix.entity.CalendarEvent;
import com.crm.matrix.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("""
        SELECT e FROM CalendarEvent e
        JOIN FETCH e.createdBy
        WHERE e.startTime <= :toDate AND e.endTime >= :fromDate
        AND (
            e.targetType = com.crm.matrix.enums.EventTargetType.ALL
            OR (e.targetType = com.crm.matrix.enums.EventTargetType.INDIVIDUAL AND e.targetId = :userId)
            OR (e.targetType = com.crm.matrix.enums.EventTargetType.TEAM AND e.targetId = :teamId)
            OR (e.targetType = com.crm.matrix.enums.EventTargetType.DEPARTMENT AND e.targetDepartment = :department)
        )
        ORDER BY e.startTime ASC
    """)
    List<CalendarEvent> findEventsForUserInRange(
            @Param("userId") Long userId,
            @Param("teamId") Long teamId,
            @Param("department") Department department,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<CalendarEvent> findAllByOrderByStartTimeDesc();

    List<CalendarEvent> findAllByOrderByStartTimeAsc();
}