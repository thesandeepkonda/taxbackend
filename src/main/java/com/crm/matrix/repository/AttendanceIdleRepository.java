package com.crm.matrix.repository;

import com.crm.matrix.entity.AttendanceIdle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceIdleRepository
        extends JpaRepository<AttendanceIdle, Long> {

    List<AttendanceIdle> findByAttendanceIdOrderByStartTimeAsc(
            Long attendanceId
    );

    Optional<AttendanceIdle> findByAttendanceIdAndEndTimeIsNull(
            Long attendanceId
    );

    boolean existsByAttendanceIdAndEndTimeIsNull(
            Long attendanceId
    );
}