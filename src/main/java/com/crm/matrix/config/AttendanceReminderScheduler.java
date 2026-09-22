package com.crm.matrix.config;

import com.crm.matrix.dto.AdminDailyAttendanceDto;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.Role;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.AttendanceService;
import com.crm.matrix.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceReminderScheduler {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Runs at the 0th second of every minute
    @Scheduled(cron = "0 * * * * ?")
    @Transactional(readOnly = true)
    public void notifyAdminsAboutShiftAbsentees() {
        LocalDate today = LocalDate.now();
        System.out.println("⏰ Scheduler ran at: " + LocalTime.now());

        // Define grace period (exactly 30 minutes after shift starts)
        // If it is 10:00 AM now, we are checking people whose shift started at 09:30 AM
        LocalTime targetShiftTime = LocalTime.now().minusMinutes(30).withSecond(0).withNano(0);

        // 1. Get today's unified attendance data (pass 'null' to get everyone)
        List<AdminDailyAttendanceDto> todaysAttendance = attendanceService.getAllCompanyAttendanceByDate(today, null);

        // 2. Filter for employees who are ABSENT AND whose shift started exactly at targetShiftTime
        List<AdminDailyAttendanceDto> newlyAbsent = todaysAttendance.stream()
                .filter(emp -> emp.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .filter(emp -> emp.getShiftStartTime() != null
                        && emp.getShiftStartTime().withSecond(0).withNano(0).equals(targetShiftTime))
                .toList();

        // 3. If there are absentees for this specific shift, notify admins
        if (!newlyAbsent.isEmpty()) {
            // Updated to use the Role Enum
            List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);

            String title = "Shift Absence Alert";
            String message = newlyAbsent.size() + " employee(s) missed their " + targetShiftTime + " shift (30 min grace period passed).";

            for (User admin : admins) {
                notificationService.sendNotification(
                        admin,
                        title,
                        message,
                        "ATTENDANCE_SUMMARY",
                        "/admin/view-attendance?tab=absents"
                );
            }
        }
    }
}