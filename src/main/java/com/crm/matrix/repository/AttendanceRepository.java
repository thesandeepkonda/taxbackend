package com.crm.matrix.repository;

import com.crm.matrix.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    boolean existsByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    List<Attendance> findByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(Long userId, LocalDate fromDate, LocalDate toDate);

    long countByAttendanceDateAndUser_ActiveTrue(LocalDate date);

    long countByAttendanceDateAndUser_TeamIdAndUser_ActiveTrue(LocalDate date, Long teamId);

    List<Attendance> findByAttendanceDate(LocalDate targetDate);
}