package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.*;
import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.LeaveRequestStatus;
import com.crm.matrix.enums.LeaveType;
import com.crm.matrix.repository.*;
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
    private final LeaveRequestRepository leaveRequestRepository; // Ensure this is injected in your constructor


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

        AttendanceBreak attendanceBreak = attendanceBreakRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).orElseThrow(() -> new IllegalStateException("No active break found"));

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

        AttendanceIdle idle = attendanceIdleRepository.findByAttendanceIdAndEndTimeIsNull(attendance.getId()).orElseThrow(() -> new IllegalStateException("No active idle period found"));

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
        return attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), LocalDate.now()).map(this::mapToResponse).orElse(null);
    }

    // =========================================================
    // HELPERS & RESPONSE MAPPING
    // =========================================================
    private Attendance getTodayAttendance(User user) {
        return attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), LocalDate.now()).orElseThrow(() -> new IllegalStateException("Employee has not checked in today"));
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
        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
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

        return AttendanceResponse.builder().id(attendance.getId()).employeeId(attendance.getUser().getId()).employeeCode(attendance.getUser().getEmployeeCode()).employeeName(fullName).attendanceDate(attendance.getAttendanceDate()).checkIn(attendance.getCheckIn()).checkOut(attendance.getCheckOut()).status(attendance.getStatus()).totalWorkMinutes(liveWorkMinutes)    // Returns live work minutes
                .totalBreakMinutes(liveBreakMinutes)  // Returns live break minutes
                .totalIdleMinutes(liveIdleMinutes)    // Returns live idle minutes
                .breakActive(breakActive).policyViolation(violation).isLate(late).isEarlyCheckout(earlyCheckout).build();
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
        String dynamicWorkingDays = (policy != null && policy.getWorkingDays() != null) ? policy.getWorkingDays() : "Not Assigned";

        return TeamAttendanceResponse.builder().id(base.getId()).employeeId(base.getEmployeeId()).employeeCode(base.getEmployeeCode()).employeeName(base.getEmployeeName()).attendanceDate(base.getAttendanceDate()).checkIn(base.getCheckIn()).checkOut(base.getCheckOut()).status(base.getStatus()).totalWorkMinutes(base.getTotalWorkMinutes()).totalBreakMinutes(base.getTotalBreakMinutes()).totalIdleMinutes(base.getTotalIdleMinutes()).breakActive(base.isBreakActive()).policyViolation(base.isPolicyViolation()).isLate(base.isLate()).isEarlyCheckout(base.isEarlyCheckout()).shiftStartTime(policy != null ? policy.getStartTime() : null).shiftEndTime(policy != null ? policy.getEndTime() : null).workingDays(dynamicWorkingDays) // <-- NOW 100% DYNAMIC
                .currentStatus(currentStatus).build();
    }

    // =========================================================
    // GET MY TEAM'S ATTENDANCE
    // =========================================================
    @Transactional(readOnly = true)
    public List<TeamAttendanceResponse> getMyTeamAttendance(LocalDate date, Authentication authentication) {
        User loggedInUser = userRepository.findByEmployeeCode(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Logged-in user not found"));

        if (loggedInUser.getTeam() == null) {
            throw new IllegalStateException("You are not assigned to any team");
        }

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<User> teamMembers = userRepository.findByTeamIdAndActiveTrue(loggedInUser.getTeam().getId());
        List<TeamAttendanceResponse> teamAttendance = new java.util.ArrayList<>();

        Long teamLeadId = (loggedInUser.getTeam().getTeamLead() != null) ? loggedInUser.getTeam().getTeamLead().getId() : null;

        LocalTime currentTime = LocalTime.now();

        for (User member : teamMembers) {
            if (member.getId().equals(teamLeadId) || member.getId().equals(loggedInUser.getId())) {
                continue;
            }

            String firstName = member.getFirstName() != null ? member.getFirstName() : "";
            String lastName = member.getLastName();
            final String fullName = (lastName != null && !lastName.isBlank()) ? firstName + " " + lastName : firstName;

            AttendancePolicy policy = member.getAttendancePolicy();
            final LocalTime shiftStartTime = policy != null ? policy.getStartTime() : null;
            final LocalTime shiftEndTime = policy != null ? policy.getEndTime() : null;

            // Dynamically pull working days from the database
            final String dynamicWorkingDays = (policy != null && policy.getWorkingDays() != null) ? policy.getWorkingDays() : "Not Assigned";

            // Calculate 'Upcoming' vs 'Off Shift' for users who haven't checked in
            String tempStatus = "Off Shift";
            if (shiftStartTime != null && currentTime.isBefore(shiftStartTime)) {
                tempStatus = "Upcoming";
            }
            final String finalStatus = tempStatus;

            attendanceRepository.findByUserIdAndAttendanceDate(member.getId(), targetDate).ifPresentOrElse(
                    // Use the mapping helper
                    attendance -> teamAttendance.add(mapToTeamResponse(attendance)),

                    // Build the DTO for absent employees
                    () -> teamAttendance.add(TeamAttendanceResponse.builder().employeeId(member.getId()).employeeCode(member.getEmployeeCode()).employeeName(fullName).attendanceDate(targetDate).status(AttendanceStatus.ABSENT).totalWorkMinutes(0L).totalBreakMinutes(0L).totalIdleMinutes(0L).breakActive(false).policyViolation(false).isLate(false).isEarlyCheckout(false).shiftStartTime(shiftStartTime).shiftEndTime(shiftEndTime).workingDays(dynamicWorkingDays) // <-- NOW 100% DYNAMIC
                            .currentStatus(finalStatus).build()));
        }

        return teamAttendance;
    }


    // =========================================================
    // ADMIN: COMPANY-WIDE ATTENDANCE SUMMARY
    // =========================================================
    @Transactional(readOnly = true)
    public AttendanceSummaryDto getAdminAttendanceSummary() {
        LocalDate today = LocalDate.now();

        long totalEmployees = userRepository.countByActiveTrue();
        long presentCount = attendanceRepository.countByAttendanceDateAndUser_ActiveTrue(today);

        // Reusing the repository method created in the previous step
        long onLeaveCount = leaveRequestRepository.findActiveLeaves(com.crm.matrix.enums.LeaveRequestStatus.APPROVED, today).size();

        long absentCount = totalEmployees - presentCount - onLeaveCount;
        if (absentCount < 0) absentCount = 0; // Failsafe

        return AttendanceSummaryDto.builder().totalEmployees(totalEmployees).present(presentCount).onLeave(onLeaveCount).absent(absentCount).build();
    }

    // =========================================================
    // TEAM LEAD: TEAM-SPECIFIC ATTENDANCE SUMMARY
    // =========================================================
    @Transactional(readOnly = true)
    public AttendanceSummaryDto getTeamAttendanceSummary(String employeeCode) {
        User teamLead = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new IllegalArgumentException("Logged-in user not found"));

        if (teamLead.getTeam() == null) {
            throw new IllegalStateException("You are not assigned to any team");
        }

        Long teamId = teamLead.getTeam().getId();
        LocalDate today = LocalDate.now();

        long totalEmployees = userRepository.countByTeamIdAndActiveTrue(teamId);
        long presentCount = attendanceRepository.countByAttendanceDateAndUser_TeamIdAndUser_ActiveTrue(today, teamId);

        // Reusing the repository method created in the previous step
        long onLeaveCount = leaveRequestRepository.findActiveLeavesByTeam(teamId, com.crm.matrix.enums.LeaveRequestStatus.APPROVED, today).size();

        long absentCount = totalEmployees - presentCount - onLeaveCount;
        if (absentCount < 0) absentCount = 0; // Failsafe

        return AttendanceSummaryDto.builder().totalEmployees(totalEmployees).present(presentCount).onLeave(onLeaveCount).absent(absentCount).build();
    }

    // =========================================================
    // GET ALL ABSENTEES BY DATE (ABSENT + ON LEAVE)
    // =========================================================
    // =========================================================
    // GET ALL ABSENTEES BY DATE (ABSENT + ON LEAVE)
    // =========================================================
//    @Transactional(readOnly = true)
//    public List<UserResponseDto> getAllAbsenteesByDate(LocalDate date) {
//        LocalDate targetDate = (date != null) ? date : LocalDate.now();
//        LocalTime currentTime = LocalTime.now();
//        boolean isToday = targetDate.equals(LocalDate.now());
//
//        // 1. Get all active non-admin employees
//        List<User> allActiveUsers = userRepository.findByActiveTrue()
//                .stream()
//                .filter(user -> user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getName()))
//                .toList();
//
//        // 2. Get IDs of employees who checked in on this date
//        List<Long> presentUserIds = attendanceRepository.findByAttendanceDate(targetDate)
//                .stream()
//                .map(att -> att.getUser().getId())
//                .toList();
//
//        // 3. Get approved leaves that cover the target date safely
//        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findAll().stream()
//                .filter(leave -> leave.getStatus() == LeaveRequestStatus.APPROVED)
//                .filter(leave -> !leave.getFromDate().isAfter(targetDate) && !leave.getToDate().isBefore(targetDate))
//                .toList();
//
//        java.util.Map<Long, LeaveType> leaveTypeMap = new java.util.HashMap<>();
//        for (LeaveRequest leave : approvedLeaves) {
//            if (leave.getUser() != null) {
//                leaveTypeMap.put(leave.getUser().getId(), leave.getLeaveType());
//            }
//        }
//
//        List<UserResponseDto> absentees = new java.util.ArrayList<>();
//
//        for (User user : allActiveUsers) {
//            // Skip if they are present
//            if (presentUserIds.contains(user.getId())) {
//                continue;
//            }
//
//            // For today, check if their shift start time has arrived (unless they are on approved leave)
//            boolean shiftStarted = true;
//            boolean isOnLeave = leaveTypeMap.containsKey(user.getId());
//
//            if (isToday && user.getAttendancePolicy() != null && user.getAttendancePolicy().getStartTime() != null) {
//                if (currentTime.isBefore(user.getAttendancePolicy().getStartTime())) {
//                    shiftStarted = false;
//                }
//            }
//
//            // If shift hasn't started yet AND they are not on leave, don't flag them yet
//            if (!shiftStarted && !isOnLeave) {
//                continue;
//            }
//
//            // Determine status and leave type explicitly
//            AttendanceStatus status = isOnLeave ? AttendanceStatus.ON_LEAVE : AttendanceStatus.ABSENT;
//            LeaveType leaveType = isOnLeave ? leaveTypeMap.get(user.getId()) : null;
//
//            absentees.add(UserResponseDto.builder()
//                    .id(user.getId())
//                    .employeeCode(user.getEmployeeCode())
//                    .firstName(user.getFirstName())
//                    .lastName(user.getLastName())
//                    .email(user.getEmail())
//                    .phone(user.getPhone())
//                    .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : "No Department")
//                    .teamName(user.getTeam() != null ? user.getTeam().getName() : "Unassigned")
//                    .attendanceStatus(status)
//                    .leaveType(leaveType)
//                    .build());
//        }
//
//        return absentees;
//    }

    // =========================================================
    // GET ALL COMPANY ATTENDANCE BY DATE (ALL DYNAMIC STATUSES)
    // =========================================================
    @Transactional(readOnly = true)
    public List<AdminDailyAttendanceDto> getAllCompanyAttendanceByDate(LocalDate date, AttendanceStatus filterStatus) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        boolean isToday = targetDate.equals(LocalDate.now());

        // 1. Get all active non-admin employees
        List<User> allActiveUsers = userRepository.findByActiveTrue().stream().filter(user -> user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getName())).toList();

        // 2. Get all attendance records for this date
        List<Attendance> attendances = attendanceRepository.findByAttendanceDate(targetDate);
        java.util.Map<Long, Attendance> attendanceMap = new java.util.HashMap<>();
        for (Attendance att : attendances) {
            attendanceMap.put(att.getUser().getId(), att);
        }

        // 3. Get approved leaves for this date safely
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findAll().stream().filter(leave -> leave.getStatus() == LeaveRequestStatus.APPROVED).filter(leave -> !leave.getFromDate().isAfter(targetDate) && !leave.getToDate().isBefore(targetDate)).toList();

        java.util.Map<Long, LeaveType> leaveTypeMap = new java.util.HashMap<>();
        for (LeaveRequest leave : approvedLeaves) {
            if (leave.getUser() != null) {
                leaveTypeMap.put(leave.getUser().getId(), leave.getLeaveType());
            }
        }

        List<AdminDailyAttendanceDto> result = new java.util.ArrayList<>();

        // 4. Combine data for all users
        for (User user : allActiveUsers) {
            Attendance attendance = attendanceMap.get(user.getId());
            boolean isOnLeave = leaveTypeMap.containsKey(user.getId());

            AttendanceStatus finalStatus;
            LeaveType finalLeaveType = null;
            LocalDateTime checkIn = null;
            LocalDateTime checkOut = null;
            Long totalWorkMinutes = 0L;

            AttendancePolicy policy = user.getAttendancePolicy();
            LocalTime shiftStartTime = (policy != null) ? policy.getStartTime() : null;
            LocalTime shiftEndTime = (policy != null) ? policy.getEndTime() : null;

            if (isOnLeave) {
                // ------------------------------------------------
                // DYNAMIC: ON LEAVE
                // ------------------------------------------------
                finalStatus = AttendanceStatus.ON_LEAVE;
                finalLeaveType = leaveTypeMap.get(user.getId());
            } else if (attendance != null) {
                checkIn = attendance.getCheckIn();
                checkOut = attendance.getCheckOut();

                if (checkIn != null && checkOut == null) {
                    // Calculate live ticking minutes
                    long sessionMinutes = Duration.between(checkIn, LocalDateTime.now()).toMinutes();
                    long breakMins = attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0L;
                    long idleMins = attendance.getTotalIdleMinutes() != null ? attendance.getTotalIdleMinutes() : 0L;
                    totalWorkMinutes = sessionMinutes - breakMins - idleMins;
                    if (totalWorkMinutes < 0) totalWorkMinutes = 0L;

                    // ------------------------------------------------
                    // DYNAMIC: NOT CHECKED OUT (vs PRESENT)
                    // ------------------------------------------------
                    if (targetDate.isBefore(LocalDate.now())) {
                        // It's a past date and they never checked out
                        finalStatus = AttendanceStatus.NOT_CHECKED_OUT;
                    } else if (isToday && shiftEndTime != null && currentTime.isAfter(shiftEndTime.plusHours(1))) {
                        // It's today, but it is way past their shift end time (1 hr grace)
                        finalStatus = AttendanceStatus.NOT_CHECKED_OUT;
                    } else {
                        // They are currently working on their shift
                        finalStatus = AttendanceStatus.PRESENT;
                    }
                } else {
                    totalWorkMinutes = attendance.getTotalWorkMinutes() != null ? attendance.getTotalWorkMinutes() : 0L;

                    // ------------------------------------------------
                    // DYNAMIC: HALF DAY (vs PRESENT)
                    // ------------------------------------------------
                    if (shiftStartTime != null && shiftEndTime != null) {
                        long expectedMinutes = Duration.between(shiftStartTime, shiftEndTime).toMinutes();
                        // If they worked less than 60% of their expected shift, flag as Half Day
                        if (totalWorkMinutes < (expectedMinutes * 0.6)) {
                            finalStatus = AttendanceStatus.HALF_DAY;
                        } else {
                            finalStatus = AttendanceStatus.PRESENT;
                        }
                    } else {
                        // Fallback if no policy assigned: Less than 4 hours (240 mins) is Half Day
                        if (totalWorkMinutes > 0 && totalWorkMinutes <= 240) {
                            finalStatus = AttendanceStatus.HALF_DAY;
                        } else {
                            finalStatus = AttendanceStatus.PRESENT;
                        }
                    }
                }
            } else {
                // ------------------------------------------------
                // DYNAMIC: ABSENT
                // ------------------------------------------------
                if (isToday && shiftStartTime != null) {
                    if (currentTime.isBefore(shiftStartTime)) {
                        // Shift hasn't even started yet today!
                        // Do not flag them as absent yet, skip adding them to the early morning absent list.
                        continue;
                    }
                }
                finalStatus = AttendanceStatus.ABSENT;
            }

            result.add(AdminDailyAttendanceDto.builder().employeeId(user.getId()).employeeCode(user.getEmployeeCode()).firstName(user.getFirstName()).lastName(user.getLastName()).email(user.getEmail()).phone(user.getPhone()).departmentName(user.getDepartment() != null ? user.getDepartment().getName() : "No Department").teamName(user.getTeam() != null ? user.getTeam().getName() : "Unassigned").attendanceStatus(finalStatus).leaveType(finalLeaveType).checkIn(checkIn).checkOut(checkOut).totalWorkMinutes(totalWorkMinutes).shiftStartTime(shiftStartTime).build());
        }

        // Apply the enum filter from the Controller
        if (filterStatus != null) {
            return result.stream().filter(dto -> dto.getAttendanceStatus() == filterStatus).toList();
        }

        return result;
    }
}