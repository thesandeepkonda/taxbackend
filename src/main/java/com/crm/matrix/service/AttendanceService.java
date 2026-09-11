package com.crm.matrix.service;

import com.crm.matrix.dto.AttendanceResponse;
import com.crm.matrix.dto.TeamAttendanceResponse;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.repository.AttendanceBreakRepository;
import com.crm.matrix.repository.AttendanceIdleRepository;
import com.crm.matrix.repository.AttendanceRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceBreakRepository attendanceBreakRepository;
    private final UserRepository userRepository;
    private final AttendanceIdleRepository attendanceIdleRepository;

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

    // =========================================================
    // HELPERS & RESPONSE MAPPING
    // =========================================================
// =========================================================
    // HELPERS & RESPONSE MAPPING
    // =========================================================
    private AttendanceResponse mapToResponse(Attendance attendance) {
        // Fetch active break/idle to calculate live durations
        var activeBreakOpt = attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId());
        boolean breakActive = activeBreakOpt.isPresent();

        var activeIdleOpt = attendanceIdleRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId());
        boolean idleActive = activeIdleOpt.isPresent();

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate Live Break Minutes
        long liveBreakMinutes = attendance.getTotalBreakMinutes() == null ? 0L : attendance.getTotalBreakMinutes();
        if (breakActive) {
            liveBreakMinutes += java.time.Duration.between(activeBreakOpt.get().getStartTime(), now).toMinutes();
        }

        // 2. Calculate Live Idle Minutes
        long liveIdleMinutes = attendance.getTotalIdleMinutes() == null ? 0L : attendance.getTotalIdleMinutes();
        if (idleActive) {
            liveIdleMinutes += java.time.Duration.between(activeIdleOpt.get().getStartTime(), now).toMinutes();
        }

        // 3. Calculate Live Work Minutes
        long liveWorkMinutes = attendance.getTotalWorkMinutes() == null ? 0L : attendance.getTotalWorkMinutes();

        // If employee has checked in but not checked out, calculate live working minutes
        if (attendance.getCheckIn() != null && attendance.getCheckOut() == null) {
            long sessionMinutes = java.time.Duration.between(attendance.getCheckIn(), now).toMinutes();
            liveWorkMinutes = sessionMinutes - liveBreakMinutes - liveIdleMinutes;
            if (liveWorkMinutes < 0) liveWorkMinutes = 0L;
        }

        AttendancePolicy policy = attendance.getUser().getAttendancePolicy();

        // 4. Break Allowance Violation Check (Using Live Minutes)
        boolean violation = false;
        if (policy != null && policy.getAllowedBreakMinutes() != null && liveBreakMinutes > policy.getAllowedBreakMinutes()) {
            violation = true;
        }

        // 5. Late Check-In Check
        boolean late = false;
        if (attendance.getCheckIn() != null && policy != null && policy.getStartTime() != null) {
            if (attendance.getCheckIn().toLocalTime().isAfter(policy.getStartTime())) {
                late = true;
            }
        }

        // 6. Early Check-Out Check
        boolean earlyCheckout = false;
        if (attendance.getCheckOut() != null && policy != null && policy.getEndTime() != null) {
            if (attendance.getCheckOut().toLocalTime().isBefore(policy.getEndTime())) {
                earlyCheckout = true;
            }
        }

        // Combine First and Last Name
        User employee = attendance.getUser();
        String fullName = employee.getFirstName();
        if (employee.getLastName() != null && !employee.getLastName().isBlank()) {
            fullName += " " + employee.getLastName();
        }

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(attendance.getUser().getId())
                .employeeCode(attendance.getUser().getEmployeeCode())
                .employeeName(fullName)
                .attendanceDate(attendance.getAttendanceDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .status(attendance.getStatus())
                .totalWorkMinutes(liveWorkMinutes)    // Returns live work minutes
                .totalBreakMinutes(liveBreakMinutes)  // Returns live break minutes
                .totalIdleMinutes(liveIdleMinutes)    // Returns live idle minutes
                .breakActive(breakActive)
                .policyViolation(violation)
                .isLate(late)
                .isEarlyCheckout(earlyCheckout)
                .build();
    }



    // =========================================================
    // TEAM ATTENDANCE HELPER
    // =========================================================
    // =========================================================
    // TEAM ATTENDANCE HELPER
    // =========================================================
    private TeamAttendanceResponse mapToTeamResponse(Attendance attendance) {
        // Reuse your existing logic to get the base calculations
        AttendanceResponse base = mapToResponse(attendance);
        AttendancePolicy policy = attendance.getUser().getAttendancePolicy();

        // Calculate Current UI Status for active check-ins
        String currentStatus = "Off Shift";
        if (base.getCheckIn() != null && base.getCheckOut() == null) {
            currentStatus = "On Shift";
        }

        // Dynamically pull working days from the database
        String dynamicWorkingDays = (policy != null && policy.getWorkingDays() != null)
                ? policy.getWorkingDays()
                : "Not Assigned";

        return TeamAttendanceResponse.builder()
                .id(base.getId())
                .employeeId(base.getEmployeeId())
                .employeeCode(base.getEmployeeCode())
                .employeeName(base.getEmployeeName())
                .attendanceDate(base.getAttendanceDate())
                .checkIn(base.getCheckIn())
                .checkOut(base.getCheckOut())
                .status(base.getStatus())
                .totalWorkMinutes(base.getTotalWorkMinutes())
                .totalBreakMinutes(base.getTotalBreakMinutes())
                .totalIdleMinutes(base.getTotalIdleMinutes())
                .breakActive(base.isBreakActive())
                .policyViolation(base.isPolicyViolation())
                .isLate(base.isLate())
                .isEarlyCheckout(base.isEarlyCheckout())
                .shiftStartTime(policy != null ? policy.getStartTime() : null)
                .shiftEndTime(policy != null ? policy.getEndTime() : null)
                .workingDays(dynamicWorkingDays) // <-- NOW 100% DYNAMIC
                .currentStatus(currentStatus)
                .build();
    }

    // =========================================================
    // GET MY TEAM'S ATTENDANCE
    // =========================================================
    @Transactional(readOnly = true)
    public List<TeamAttendanceResponse> getMyTeamAttendance(LocalDate date, Authentication authentication) {
        User loggedInUser = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found"));

        if (loggedInUser.getTeam() == null) {
            throw new IllegalStateException("You are not assigned to any team");
        }

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<User> teamMembers = userRepository.findByTeamIdAndActiveTrue(loggedInUser.getTeam().getId());
        List<TeamAttendanceResponse> teamAttendance = new java.util.ArrayList<>();

        Long teamLeadId = (loggedInUser.getTeam().getTeamLead() != null)
                ? loggedInUser.getTeam().getTeamLead().getId()
                : null;

        LocalTime currentTime = LocalTime.now();

        for (User member : teamMembers) {
            if (member.getId().equals(teamLeadId) || member.getId().equals(loggedInUser.getId())) {
                continue;
            }

            String firstName = member.getFirstName() != null ? member.getFirstName() : "";
            String lastName = member.getLastName();
            final String fullName = (lastName != null && !lastName.isBlank())
                    ? firstName + " " + lastName
                    : firstName;

            AttendancePolicy policy = member.getAttendancePolicy();
            final LocalTime shiftStartTime = policy != null ? policy.getStartTime() : null;
            final LocalTime shiftEndTime = policy != null ? policy.getEndTime() : null;

            // Dynamically pull working days from the database
            final String dynamicWorkingDays = (policy != null && policy.getWorkingDays() != null)
                    ? policy.getWorkingDays()
                    : "Not Assigned";

            // Calculate 'Upcoming' vs 'Off Shift' for users who haven't checked in
            String tempStatus = "Off Shift";
            if (shiftStartTime != null && currentTime.isBefore(shiftStartTime)) {
                tempStatus = "Upcoming";
            }
            final String finalStatus = tempStatus;

            attendanceRepository.findByUserIdAndAttendanceDate(member.getId(), targetDate)
                    .ifPresentOrElse(
                            // Use the mapping helper
                            attendance -> teamAttendance.add(mapToTeamResponse(attendance)),

                            // Build the DTO for absent employees
                            () -> teamAttendance.add(TeamAttendanceResponse.builder()
                                    .employeeId(member.getId())
                                    .employeeCode(member.getEmployeeCode())
                                    .employeeName(fullName)
                                    .attendanceDate(targetDate)
                                    .status(AttendanceStatus.ABSENT)
                                    .totalWorkMinutes(0L)
                                    .totalBreakMinutes(0L)
                                    .totalIdleMinutes(0L)
                                    .breakActive(false)
                                    .policyViolation(false)
                                    .isLate(false)
                                    .isEarlyCheckout(false)
                                    .shiftStartTime(shiftStartTime)
                                    .shiftEndTime(shiftEndTime)
                                    .workingDays(dynamicWorkingDays) // <-- NOW 100% DYNAMIC
                                    .currentStatus(finalStatus)
                                    .build()
                            )
                    );
        }

        return teamAttendance;
    }
}