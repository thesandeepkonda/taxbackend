package com.crm.matrix.service;

import com.crm.matrix.dto.AttendanceResponse;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.repository.AttendanceBreakRepository;
import com.crm.matrix.repository.AttendanceIdleRepository;
import com.crm.matrix.repository.AttendanceRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceBreakRepository attendanceBreakRepository;
    private final UserRepository userRepository;
    private final AttendanceIdleRepository attendanceIdleRepository;

    // =========================================================
    // CHECK IN
    // =========================================================
    @Transactional
    public AttendanceResponse checkIn(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (attendanceRepository.existsByUserIdAndAttendanceDate(user.getId(), today)) {
            throw new IllegalStateException("Employee has already checked in today");
        }

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(now);
        attendance.setCheckOut(null);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setTotalWorkMinutes(0L);
        attendance.setTotalBreakMinutes(0L);
        attendance.setTotalIdleMinutes(0L);

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    // =========================================================
    // START BREAK
    // =========================================================
    @Transactional
    public AttendanceResponse startBreak(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        Attendance attendance = getTodayAttendance(user);
        validateAttendanceOpen(attendance);

        if (attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent()) {
            throw new IllegalStateException("Employee is already on a break");
        }

        AttendanceBreak attendanceBreak = new AttendanceBreak();
        attendanceBreak.setAttendance(attendance);
        attendanceBreak.setStartTime(LocalDateTime.now());
        attendanceBreakRepository.save(attendanceBreak);

        return mapToResponse(attendance);
    }

    // =========================================================
    // END BREAK
    // =========================================================
    @Transactional
    public AttendanceResponse endBreak(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        Attendance attendance = getTodayAttendance(user);
        validateAttendanceOpen(attendance);

        AttendanceBreak attendanceBreak = attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId())
                .orElseThrow(() -> new IllegalStateException("No active break found"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(attendanceBreak.getStartTime())) {
            throw new IllegalStateException("Break end time cannot be before break start time");
        }

        long durationMinutes = Duration.between(attendanceBreak.getStartTime(), now).toMinutes();
        attendanceBreak.setEndTime(now);
        attendanceBreak.setDurationMinutes(durationMinutes);
        attendanceBreakRepository.save(attendanceBreak);

        long currentBreakMinutes = attendance.getTotalBreakMinutes() == null ? 0L : attendance.getTotalBreakMinutes();
        attendance.setTotalBreakMinutes(currentBreakMinutes + durationMinutes);
        attendanceRepository.save(attendance);

        return mapToResponse(attendance);
    }

    // =========================================================
    // START IDLE
    // =========================================================
    @Transactional
    public AttendanceResponse startIdle(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        Attendance attendance = getTodayAttendance(user);
        validateAttendanceOpen(attendance);

        if (attendanceIdleRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent()) {
            throw new IllegalStateException("Employee is already idle");
        }
        if (attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent()) {
            throw new IllegalStateException("Employee is currently on break");
        }

        AttendanceIdle idle = new AttendanceIdle();
        idle.setAttendance(attendance);
        idle.setStartTime(LocalDateTime.now());
        attendanceIdleRepository.save(idle);

        return mapToResponse(attendance);
    }

    // =========================================================
    // END IDLE
    // =========================================================
    @Transactional
    public AttendanceResponse endIdle(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        Attendance attendance = getTodayAttendance(user);
        validateAttendanceOpen(attendance);

        AttendanceIdle idle = attendanceIdleRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId())
                .orElseThrow(() -> new IllegalStateException("No active idle period found"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(idle.getStartTime())) {
            throw new IllegalStateException("Idle end time cannot be before idle start time");
        }

        long durationMinutes = Duration.between(idle.getStartTime(), now).toMinutes();
        idle.setEndTime(now);
        idle.setDurationMinutes(durationMinutes);
        attendanceIdleRepository.save(idle);

        long currentIdleMinutes = attendance.getTotalIdleMinutes() == null ? 0L : attendance.getTotalIdleMinutes();
        attendance.setTotalIdleMinutes(currentIdleMinutes + durationMinutes);
        attendanceRepository.save(attendance);

        return mapToResponse(attendance);
    }

    // =========================================================
    // CHECK OUT
    // =========================================================
    @Transactional
    public AttendanceResponse checkOut(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        Attendance attendance = getTodayAttendance(user);
        validateAttendanceOpen(attendance);

        if (attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent()) {
            throw new IllegalStateException("Cannot check out while employee is on break. End the break first.");
        }
        if (attendanceIdleRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent()) {
            throw new IllegalStateException("Cannot check out while employee is idle. Resume activity first.");
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        if (checkOutTime.isBefore(attendance.getCheckIn())) {
            throw new IllegalStateException("Check-out time cannot be before check-in time");
        }

        long sessionMinutes = Duration.between(attendance.getCheckIn(), checkOutTime).toMinutes();
        long breakMinutes = attendance.getTotalBreakMinutes() == null ? 0L : attendance.getTotalBreakMinutes();
        long idleMinutes = attendance.getTotalIdleMinutes() == null ? 0L : attendance.getTotalIdleMinutes();

        long totalWorkMinutes = sessionMinutes - breakMinutes - idleMinutes;
        if (totalWorkMinutes < 0) totalWorkMinutes = 0;

        attendance.setCheckOut(checkOutTime);
        attendance.setTotalWorkMinutes(totalWorkMinutes);
        attendance.setStatus(AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getTodayStatus(String employeeCode) {
        User user = getActiveEmployee(employeeCode);
        return attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), LocalDate.now())
                .map(this::mapToResponse)
                .orElse(null);
    }

    // =========================================================
    // HELPERS & RESPONSE MAPPING
    // =========================================================
    private Attendance getTodayAttendance(User user) {
        return attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("Employee has not checked in today"));
    }

    private void validateAttendanceOpen(Attendance attendance) {
        if (attendance.getCheckIn() == null) {
            throw new IllegalStateException("Employee has not checked in");
        }
        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Attendance has already been checked out");
        }
    }

    private User getActiveEmployee(String employeeCode) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Employee is inactive");
        }
        return user;
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {
        boolean breakActive = attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).isPresent();
        AttendancePolicy policy = attendance.getUser().getAttendancePolicy();

        // 1. Break Allowance Violation Check
        boolean violation = false;
        long totalBreak = attendance.getTotalBreakMinutes() == null ? 0L : attendance.getTotalBreakMinutes();
        if (policy != null && policy.getAllowedBreakMinutes() != null && totalBreak > policy.getAllowedBreakMinutes()) {
            violation = true;
        }

        // 2. Late Check-In Check
        boolean late = false;
        if (attendance.getCheckIn() != null && policy != null && policy.getStartTime() != null) {
            if (attendance.getCheckIn().toLocalTime().isAfter(policy.getStartTime())) {
                late = true;
            }
        }

        // 3. Early Check-Out Check
        boolean earlyCheckout = false;
        if (attendance.getCheckOut() != null && policy != null && policy.getEndTime() != null) {
            if (attendance.getCheckOut().toLocalTime().isBefore(policy.getEndTime())) {
                earlyCheckout = true;
            }
        }

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(attendance.getUser().getId())
                .employeeCode(attendance.getUser().getEmployeeCode())
                .attendanceDate(attendance.getAttendanceDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .status(attendance.getStatus())
                .totalWorkMinutes(attendance.getTotalWorkMinutes())
                .totalBreakMinutes(attendance.getTotalBreakMinutes())
                .totalIdleMinutes(attendance.getTotalIdleMinutes())
                .breakActive(breakActive)
                 .policyViolation(violation)
                 .isLate(late)
                 .isEarlyCheckout(earlyCheckout)
                .build();
    }
}