package com.crm.matrix.service;

import com.crm.matrix.dto.AttendanceCalendarResponse;
import com.crm.matrix.entity.Attendance;
import com.crm.matrix.entity.LeaveRequest;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.AttendanceStatus;
import com.crm.matrix.enums.LeaveRequestStatus;
import com.crm.matrix.repository.AttendanceRepository;
import com.crm.matrix.repository.LeaveRequestRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceCalendarService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;


    // =========================================================
    // EMPLOYEE CALENDAR
    // =========================================================

    @Transactional(readOnly = true)
    public List<AttendanceCalendarResponse> getEmployeeCalendar(
            String employeeCode,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        // -----------------------------------------------------
        // DATE VALIDATION
        // -----------------------------------------------------

        if (fromDate == null) {

            throw new IllegalArgumentException(
                    "From date is required"
            );
        }

        if (toDate == null) {

            throw new IllegalArgumentException(
                    "To date is required"
            );
        }

        if (toDate.isBefore(fromDate)) {

            throw new IllegalArgumentException(
                    "To date cannot be before from date"
            );
        }


        // -----------------------------------------------------
        // OPTIONAL SAFETY LIMIT
        // -----------------------------------------------------

        if (fromDate.plusMonths(3).isBefore(toDate)) {

            throw new IllegalArgumentException(
                    "Calendar date range cannot exceed 3 months"
            );
        }


        // -----------------------------------------------------
        // FIND EMPLOYEE
        // -----------------------------------------------------

        User employee =
                userRepository
                        .findByEmployeeCode(employeeCode)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found: "
                                                + employeeCode
                                )
                        );


        // -----------------------------------------------------
        // FIND ATTENDANCE
        // ONE DATABASE QUERY
        // -----------------------------------------------------

        List<Attendance> attendances =
                attendanceRepository
                        .findByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                                employee.getId(),
                                fromDate,
                                toDate
                        );


        // -----------------------------------------------------
        // FIND APPROVED LEAVES
        // ONE DATABASE QUERY
        //
        // Gets leaves that overlap the requested period.
        // -----------------------------------------------------

        List<LeaveRequest> approvedLeaves =
                leaveRequestRepository
                        .findByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                                employee.getId(),
                                LeaveRequestStatus.APPROVED,
                                toDate,
                                fromDate
                        );


        // -----------------------------------------------------
        // CONVERT ATTENDANCE TO MAP
        // -----------------------------------------------------

        Map<LocalDate, Attendance> attendanceMap =
                new HashMap<>();


        for (Attendance attendance : attendances) {

            attendanceMap.put(
                    attendance.getAttendanceDate(),
                    attendance
            );
        }


        // -----------------------------------------------------
        // BUILD CALENDAR
        // -----------------------------------------------------

        List<AttendanceCalendarResponse> result =
                new ArrayList<>();


        LocalDate currentDate =
                fromDate;


        while (!currentDate.isAfter(toDate)) {

            /*
             * IMPORTANT:
             *
             * This variable is final/effectively final.
             * Therefore it can safely be used inside
             * lambda expressions.
             */
            final LocalDate date =
                    currentDate;


            // -------------------------------------------------
            // GET ATTENDANCE FROM MAP
            // -------------------------------------------------

            Attendance attendance =
                    attendanceMap.get(date);


            // -------------------------------------------------
            // FIND APPROVED LEAVE FOR THIS DATE
            //
            // This is now done in memory.
            // No database query here.
            // -------------------------------------------------

            LeaveRequest leave =
                    approvedLeaves
                            .stream()
                            .filter(request ->
                                    !request
                                            .getFromDate()
                                            .isAfter(date)
                                            &&
                                            !request
                                                    .getToDate()
                                                    .isBefore(date)
                            )
                            .findFirst()
                            .orElse(null);


            // -------------------------------------------------
            // DETERMINE STATUS
            // -------------------------------------------------

            AttendanceStatus status =
                    determineStatus(
                            attendance,
                            leave,
                            date
                    );


            // -------------------------------------------------
            // BUILD RESPONSE
            // -------------------------------------------------

            result.add(
                    buildCalendarResponse(
                            date,
                            attendance,
                            leave,
                            employee,
                            status
                    )
            );


            currentDate =
                    currentDate.plusDays(1);
        }


        return result;
    }


    // =========================================================
    // DETERMINE STATUS
    // =========================================================

    private AttendanceStatus determineStatus(
            Attendance attendance,
            LeaveRequest leave,
            LocalDate date
    ) {

        /*
         * ACTUAL ATTENDANCE HAS PRIORITY.
         *
         * Example:
         *
         * Employee has approved leave
         * BUT employee actually checked in.
         *
         * We show the actual attendance status.
         */

        if (attendance != null) {

            return attendance.getStatus();
        }


        // -----------------------------------------------------
        // APPROVED LEAVE
        // -----------------------------------------------------

        if (leave != null) {

            return AttendanceStatus.ON_LEAVE;
        }


        // -----------------------------------------------------
        // FUTURE DATE
        // -----------------------------------------------------

        /*
         * Don't mark future dates as ABSENT.
         *
         * Example:
         *
         * Today = Aug 31
         *
         * Sep 10 should not be ABSENT yet.
         *
         * There is no UPCOMING enum in your current
         * AttendanceStatus, so we return null.
         *
         * Frontend can display it as "Upcoming".
         */

        if (date.isAfter(LocalDate.now())) {

            return null;
        }


        // -----------------------------------------------------
        // NO ATTENDANCE + NO LEAVE
        // -----------------------------------------------------

        return AttendanceStatus.ABSENT;
    }


    // =========================================================
    // BUILD CALENDAR RESPONSE
    // =========================================================

    private AttendanceCalendarResponse buildCalendarResponse(
            LocalDate date,
            Attendance attendance,
            LeaveRequest leave,
            User employee,
            AttendanceStatus status
    ) {

        Long totalWorkMinutes = 0L;

        Long totalBreakMinutes = 0L;

        Long totalIdleMinutes = 0L;


        if (attendance != null) {

            totalWorkMinutes =
                    attendance.getTotalWorkMinutes() == null
                            ? 0L
                            : attendance.getTotalWorkMinutes();

            totalBreakMinutes =
                    attendance.getTotalBreakMinutes() == null
                            ? 0L
                            : attendance.getTotalBreakMinutes();

            totalIdleMinutes =
                    attendance.getTotalIdleMinutes() == null
                            ? 0L
                            : attendance.getTotalIdleMinutes();
        }


        // -----------------------------------------------------
        // EMPLOYEE NAME
        // -----------------------------------------------------

        String employeeName =
                employee.getFirstName();


        if (employee.getLastName() != null
                && !employee.getLastName().isBlank()) {

            employeeName =
                    employeeName
                            + " "
                            + employee.getLastName();
        }


        return AttendanceCalendarResponse.builder()

                // -------------------------------------------------
                // DATE
                // -------------------------------------------------

                .date(date)


                // -------------------------------------------------
                // STATUS
                // -------------------------------------------------

                .status(status)


                // -------------------------------------------------
                // CHECK IN
                // -------------------------------------------------

                .checkIn(
                        attendance != null
                                ? attendance.getCheckIn()
                                : null
                )


                // -------------------------------------------------
                // CHECK OUT
                // -------------------------------------------------

                .checkOut(
                        attendance != null
                                ? attendance.getCheckOut()
                                : null
                )


                // -------------------------------------------------
                // TOTAL WORK
                // -------------------------------------------------

                .totalWorkMinutes(
                        totalWorkMinutes
                )


                // -------------------------------------------------
                // TOTAL BREAK
                // -------------------------------------------------

                .totalBreakMinutes(
                        totalBreakMinutes
                )


                // -------------------------------------------------
                // TOTAL IDLE
                // -------------------------------------------------

                .totalIdleMinutes(
                        totalIdleMinutes
                )


                // -------------------------------------------------
                // LEAVE
                // -------------------------------------------------

                .onLeave(
                        leave != null
                )


                .leaveType(
                        leave != null
                                ? leave.getLeaveType()
                                : null
                )


                .leaveDescription(
                        leave != null
                                ? leave.getDescription()
                                : null
                )


                // -------------------------------------------------
                // WORK MODE
                // -------------------------------------------------

                .workMode(
                        employee.getWorkMode() != null
                                ? employee
                                .getWorkMode()
                                .name()
                                : null
                )


                .build();
    }
}