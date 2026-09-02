package com.crm.matrix.controller;

import com.crm.matrix.dto.AttendanceCalendarResponse;
import com.crm.matrix.service.AttendanceCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceCalendarController {

    private final AttendanceCalendarService attendanceCalendarService;


    @GetMapping("/calendar")
    public ResponseEntity<List<AttendanceCalendarResponse>>
    getCalendar(
            Authentication authentication,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        return ResponseEntity.ok(
                attendanceCalendarService
                        .getEmployeeCalendar(
                                authentication.getName(),
                                fromDate,
                                toDate
                        )
        );
    }
}