package com.crm.matrix.controller;

import com.crm.matrix.dto.AttendanceResponse;
import com.crm.matrix.dto.TeamAttendanceResponse;
import com.crm.matrix.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;


    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.checkIn(authentication.getName()));
    }


    @PostMapping("/break/start")
    public ResponseEntity<AttendanceResponse> startBreak(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.startBreak(authentication.getName()));
    }


    @PostMapping("/break/end")
    public ResponseEntity<AttendanceResponse> endBreak(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.endBreak(authentication.getName()));
    }

    @PostMapping("/idle/start")
    public ResponseEntity<AttendanceResponse> startIdle(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.startIdle(authentication.getName()));
    }


    @PostMapping("/idle/end")
    public ResponseEntity<AttendanceResponse> endIdle(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.endIdle(authentication.getName()));
    }


    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(Authentication authentication) {

        return ResponseEntity.ok(attendanceService.checkOut(authentication.getName()));
    }
    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> getTodayStatus(Authentication authentication) {
        AttendanceResponse response = attendanceService.getTodayStatus(authentication.getName());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
    @GetMapping("/my-team")
    public ResponseEntity<List<TeamAttendanceResponse>> getMyTeamAttendance(
            @RequestParam(required = false) LocalDate date,
            Authentication authentication) {

        return ResponseEntity.ok(attendanceService.getMyTeamAttendance(date, authentication));
    }
}