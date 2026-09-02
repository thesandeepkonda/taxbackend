package com.crm.matrix.repository;

import com.crm.matrix.entity.AttendanceBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceBreakRepository
        extends JpaRepository<AttendanceBreak, Long> {

    List<AttendanceBreak> findByAttendanceIdOrderByStartTimeAsc(
            Long attendanceId
    );

    Optional<AttendanceBreak>
    findByAttendanceIdAndEndTimeIsNull(
            Long attendanceId
    );
}