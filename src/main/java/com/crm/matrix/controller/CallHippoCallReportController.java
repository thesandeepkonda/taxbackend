package com.crm.matrix.controller;

import com.crm.matrix.dto.CallDetailResponse;
import com.crm.matrix.dto.CallReportResponse;
import com.crm.matrix.dto.EmployeeClientCallReportResponse;
import com.crm.matrix.service.CallHippoCallReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/callhippo")
@RequiredArgsConstructor
public class CallHippoCallReportController {


    private final CallHippoCallReportService callReportService;


    // =========================================================
    // ADMIN
    // EMPLOYEE REPORTS
    // =========================================================

    @GetMapping("/admin/call-report/employees")
    public ResponseEntity<List<CallReportResponse>>
    getAdminEmployeeReports(

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getAdminEmployeeReports(
                                from,
                                to
                        )
        );
    }


    // =========================================================
    // ADMIN
    // EMPLOYEE -> CLIENTS
    // PAGINATED
    // =========================================================

    @GetMapping(
            "/admin/call-report/employees/{employeeId}/clients"
    )
    public ResponseEntity<
            Page<EmployeeClientCallReportResponse>>
    getAdminEmployeeClients(

            @PathVariable Long employeeId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getAdminEmployeeClients(
                                employeeId,
                                from,
                                to,
                                page,
                                size
                        )
        );
    }


    // =========================================================
    // ADMIN
    // EMPLOYEE + CLIENT -> CALLS
    // =========================================================

    @GetMapping(
            "/admin/call-report/employees/{employeeId}/clients/{clientId}/calls"
    )
    public ResponseEntity<Page<CallDetailResponse>>
    getAdminEmployeeClientCalls(

            @PathVariable Long employeeId,

            @PathVariable Long clientId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getAdminEmployeeClientCalls(
                                employeeId,
                                clientId,
                                from,
                                to,
                                page,
                                size
                        )
        );
    }




    @GetMapping("/admin/clients/{clientId}/calls")
    public ResponseEntity<Page<CallDetailResponse>>
    getAdminClientCalls(

            @PathVariable Long clientId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService.getAdminClientCalls(
                        clientId,
                        from,
                        to,
                        page,
                        size
                )
        );
    }




    @GetMapping("/admin/calls")
    public ResponseEntity<Page<CallDetailResponse>>
    getAllCalls(

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService.getAllCalls(
                        from,
                        to,
                        page,
                        size
                )
        );
    }




    @GetMapping(
            "/team-lead/call-report/employees"
    )
    public ResponseEntity<List<CallReportResponse>>
    getTeamEmployeeReports(

            Authentication authentication,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getTeamEmployeeReports(
                                authentication,
                                from,
                                to
                        )
        );
    }


    // =========================================================
    // TEAM LEAD
    // EMPLOYEE -> CLIENTS
    // =========================================================

    @GetMapping(
            "/team-lead/call-report/employees/{employeeId}/clients"
    )
    public ResponseEntity<
            Page<EmployeeClientCallReportResponse>>
    getTeamEmployeeClients(

            Authentication authentication,

            @PathVariable Long employeeId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getTeamEmployeeClients(
                                authentication,
                                employeeId,
                                from,
                                to,
                                page,
                                size
                        )
        );
    }


    // =========================================================
    // TEAM LEAD
    // EMPLOYEE + CLIENT -> CALLS
    // =========================================================

    @GetMapping(
            "/team-lead/call-report/employees/{employeeId}/clients/{clientId}/calls"
    )
    public ResponseEntity<Page<CallDetailResponse>>
    getTeamEmployeeClientCalls(

            Authentication authentication,

            @PathVariable Long employeeId,

            @PathVariable Long clientId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        LocalDate from =
                parseDate(startDate);

        LocalDate to =
                parseDate(endDate);

        return ResponseEntity.ok(
                callReportService
                        .getTeamEmployeeClientCalls(
                                authentication,
                                employeeId,
                                clientId,
                                from,
                                to,
                                page,
                                size
                        )
        );
    }


    // =========================================================
    // DATE PARSER
    // =========================================================

    private LocalDate parseDate(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        return LocalDate.parse(value);
    }
}