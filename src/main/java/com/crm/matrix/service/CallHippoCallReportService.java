package com.crm.matrix.service;

import com.crm.matrix.dto.CallDetailResponse;
import com.crm.matrix.dto.CallReportResponse;
import com.crm.matrix.dto.EmployeeClientCallReportResponse;
import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CallHippoCallReportService {

    private final CallHistoryRepository callHistoryRepository;

    private final UserRepository userRepository;

    private final ClientRepository clientRepository;


    // =========================================================
    // ADMIN
    // ALL EMPLOYEE REPORTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<CallReportResponse> getAdminEmployeeReports(
            LocalDate from,
            LocalDate to
    ) {

        List<User> users =
                userRepository.findAll();

        List<CallReportResponse> result =
                new ArrayList<>();

        for (User user : users) {

            List<CallHistory> calls =
                    getCallsForEmployee(
                            user.getId(),
                            from,
                            to
                    );

            if (calls.isEmpty()) {
                continue;
            }

            result.add(
                    buildEmployeeReport(
                            user,
                            calls
                    )
            );
        }

        return result;
    }


    // =========================================================
    // ADMIN
    // EMPLOYEE -> CLIENT SUMMARY
    // PAGINATED
    // =========================================================

    @Transactional(readOnly = true)
    public Page<EmployeeClientCallReportResponse> getAdminEmployeeClients(
            Long employeeId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        // ---------------------------------------------------------
        // 1. Verify employee exists
        // ---------------------------------------------------------

        User employee =
                userRepository
                        .findById(employeeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found: " + employeeId
                                )
                        );

        // ---------------------------------------------------------
        // 2. Validate pagination
        // ---------------------------------------------------------

        if (page < 0) {
            throw new RuntimeException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0) {
            throw new RuntimeException(
                    "Page size must be greater than 0"
            );
        }

        // ---------------------------------------------------------
        // 3. Get ALL calls for this employee/date range
        //
        // IMPORTANT:
        // Do NOT paginate CallHistory here.
        // We need all calls so that client statistics are correct.
        // ---------------------------------------------------------

        List<CallHistory> calls =
                getCallsForEmployee(
                        employeeId,
                        from,
                        to
                );

        // ---------------------------------------------------------
        // 4. Group ALL calls by client
        // ---------------------------------------------------------

        List<EmployeeClientCallReportResponse> allClientReports =
                buildClientReports(
                        calls
                );

        // ---------------------------------------------------------
        // 5. Paginate the UNIQUE CLIENTS
        // ---------------------------------------------------------

        int totalClients =
                allClientReports.size();

        int start =
                page * size;

        if (start >= totalClients) {

            return new org.springframework.data.domain.PageImpl<>(
                    List.of(),
                    PageRequest.of(page, size),
                    totalClients
            );
        }

        int end =
                Math.min(
                        start + size,
                        totalClients
                );

        List<EmployeeClientCallReportResponse> pageContent =
                allClientReports.subList(
                        start,
                        end
                );

        // ---------------------------------------------------------
        // 6. Return Page
        // ---------------------------------------------------------

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return new org.springframework.data.domain.PageImpl<>(
                pageContent,
                pageable,
                totalClients
        );
    }

    private List<EmployeeClientCallReportResponse>
    buildClientReports(
            List<CallHistory> calls
    ) {

        return calls
                .stream()
                .filter(
                        call ->
                                call.getClient() != null
                )
                .collect(
                        java.util.stream.Collectors
                                .groupingBy(
                                        call ->
                                                call.getClient()
                                                        .getId()
                                )
                )
                .values()
                .stream()
                .map(
                        this::buildClientReport
                )
                .sorted(
                        java.util.Comparator.comparing(
                                EmployeeClientCallReportResponse::getLastCallTime,
                                java.util.Comparator.nullsLast(
                                        java.util.Comparator.reverseOrder()
                                )
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CallDetailResponse>
    getAdminEmployeeClientCalls(
            Long employeeId,
            Long clientId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        validateEmployee(employeeId);

        // IMPORTANT:
        // Search client in ClientRepository
        Client client =
                validateClient(clientId);

        Pageable pageable =
                createPageable(page, size);

        Page<CallHistory> calls;

        if (from != null && to != null) {

            calls =
                    callHistoryRepository
                            .findByUserIdAndClientIdAndCallTimeBetween(
                                    employeeId,
                                    client.getId(),
                                    from.atStartOfDay(),
                                    to.atTime(LocalTime.MAX),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findByUserIdAndClientId(
                                    employeeId,
                                    client.getId(),
                                    pageable
                            );
        }

        return calls.map(
                CallDetailResponse::from
        );
    }


    // =========================================================
    // ADMIN
    // ALL CLIENT CALL HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CallDetailResponse>
    getAdminClientCalls(
            Long clientId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        // Search client in ClientRepository
        Client client =
                validateClient(clientId);

        Pageable pageable =
                createPageable(page, size);

        Page<CallHistory> calls;

        if (from != null && to != null) {

            calls =
                    callHistoryRepository
                            .findByClientIdAndCallTimeBetween(
                                    client.getId(),
                                    from.atStartOfDay(),
                                    to.atTime(LocalTime.MAX),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findByClientId(
                                    client.getId(),
                                    pageable
                            );
        }

        return calls.map(
                CallDetailResponse::from
        );
    }


    // =========================================================
    // ADMIN
    // ALL CALL HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CallDetailResponse>
    getAllCalls(
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        Pageable pageable =
                createPageable(page, size);

        Page<CallHistory> calls;

        if (from != null && to != null) {

            calls =
                    callHistoryRepository
                            .findByCallTimeBetween(
                                    from.atStartOfDay(),
                                    to.atTime(LocalTime.MAX),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findAllBy(
                                    pageable
                            );
        }

        return calls.map(
                CallDetailResponse::from
        );
    }



    // =========================================================
    // TEAM LEAD
    // TEAM EMPLOYEE REPORTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<CallReportResponse>
    getTeamEmployeeReports(
            Authentication authentication,
            LocalDate from,
            LocalDate to
    ) {

        User teamLead =
                getLoggedInUser(
                        authentication
                );

        if (teamLead.getTeam() == null) {

            throw new RuntimeException(
                    "Team Lead is not assigned to a team"
            );
        }

        Long teamId =
                teamLead.getTeam().getId();

        List<User> users =
                userRepository.findByTeamId(
                        teamId
                );

        List<CallReportResponse> result =
                new ArrayList<>();

        for (User user : users) {

            List<CallHistory> calls =
                    getCallsForEmployee(
                            user.getId(),
                            from,
                            to
                    );

            if (!calls.isEmpty()) {

                result.add(
                        buildEmployeeReport(
                                user,
                                calls
                        )
                );
            }
        }

        return result;
    }


    // =========================================================
    // TEAM LEAD
    // EMPLOYEE -> CLIENTS
    // PAGINATED
    // =========================================================

    @Transactional(readOnly = true)
    public Page<EmployeeClientCallReportResponse>
    getTeamEmployeeClients(
            Authentication authentication,
            Long employeeId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        User teamLead =
                getLoggedInUser(
                        authentication
                );

        validateTeamEmployee(
                teamLead,
                employeeId
        );

        Pageable pageable =
                createPageable(page, size);

        Page<CallHistory> calls;

        if (from != null && to != null) {

            calls =
                    callHistoryRepository
                            .findByUserIdAndCallTimeBetween(
                                    employeeId,
                                    from.atStartOfDay(),
                                    to.atTime(LocalTime.MAX),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findByUserId(
                                    employeeId,
                                    pageable
                            );
        }

        return buildClientReportsPage(calls);
    }


    // =========================================================
    // TEAM LEAD
    // EMPLOYEE + CLIENT -> CALLS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CallDetailResponse>
    getTeamEmployeeClientCalls(
            Authentication authentication,
            Long employeeId,
            Long clientId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        User teamLead =
                getLoggedInUser(
                        authentication
                );

        validateTeamEmployee(
                teamLead,
                employeeId
        );

        // Search client in ClientRepository
        Client client =
                validateClient(clientId);

        Pageable pageable =
                createPageable(page, size);

        Page<CallHistory> calls;

        if (from != null && to != null) {

            calls =
                    callHistoryRepository
                            .findByUserIdAndClientIdAndCallTimeBetween(
                                    employeeId,
                                    client.getId(),
                                    from.atStartOfDay(),
                                    to.atTime(LocalTime.MAX),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findByUserIdAndClientId(
                                    employeeId,
                                    client.getId(),
                                    pageable
                            );
        }

        return calls.map(
                CallDetailResponse::from
        );
    }


    // =========================================================
    // BUILD EMPLOYEE REPORT
    // =========================================================

    private CallReportResponse buildEmployeeReport(
            User user,
            List<CallHistory> calls
    ) {

        long totalCalls =
                calls.size();

        long answeredCalls =
                calls.stream()
                        .filter(this::isAnswered)
                        .count();

        long failedCalls =
                calls.stream()
                        .filter(this::isFailed)
                        .count();

        long notAnsweredCalls =
                totalCalls
                        - answeredCalls
                        - failedCalls;

        long totalSeconds =
                calls.stream()
                        .map(CallHistory::getDurationSeconds)
                        .filter(value -> value != null)
                        .mapToLong(Integer::longValue)
                        .sum();

        long averageSeconds =
                answeredCalls == 0
                        ? 0
                        : totalSeconds / answeredCalls;

        LocalDateTime firstCall =
                calls.stream()
                        .map(CallHistory::getCallTime)
                        .filter(value -> value != null)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

        LocalDateTime lastCall =
                calls.stream()
                        .map(CallHistory::getCallTime)
                        .filter(value -> value != null)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

        long totalClients =
                calls.stream()
                        .map(CallHistory::getClient)
                        .filter(client -> client != null)
                        .map(Client::getId)
                        .distinct()
                        .count();

        String employeeName =
                (
                        user.getFirstName() == null
                                ? ""
                                : user.getFirstName()
                )
                        + " "
                        +
                        (
                                user.getLastName() == null
                                        ? ""
                                        : user.getLastName()
                        );

        employeeName =
                employeeName.trim();

        return new CallReportResponse(

                user.getId(),

                user.getEmployeeCode(),

                employeeName,

                totalClients,

                totalCalls,

                answeredCalls,

                notAnsweredCalls,

                failedCalls,

                totalSeconds,

                totalSeconds / 60,

                averageSeconds,

                firstCall,

                lastCall
        );
    }


    // =========================================================
    // BUILD CLIENT REPORT PAGE
    // =========================================================

    private Page<EmployeeClientCallReportResponse>
    buildClientReportsPage(
            Page<CallHistory> calls
    ) {

        /*
         * Because the repository page contains individual calls,
         * we group calls by client.
         *
         * Pagination is therefore applied to CALL HISTORY rows,
         * not guaranteed unique clients per page.
         *
         * If you need true "one client = one page row",
         * use a dedicated GROUP BY query.
         */

        List<EmployeeClientCallReportResponse> result =
                calls.getContent()
                        .stream()
                        .filter(
                                call ->
                                        call.getClient() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        call ->
                                                call.getClient()
                                                        .getId()
                                )
                        )
                        .values()
                        .stream()
                        .map(
                                this::buildClientReport
                        )
                        .toList();

        return new org.springframework.data.domain.PageImpl<>(
                result,
                calls.getPageable(),
                calls.getTotalElements()
        );
    }


    // =========================================================
    // BUILD CLIENT REPORT
    // =========================================================

    private EmployeeClientCallReportResponse
    buildClientReport(
            List<CallHistory> calls
    ) {

        CallHistory first =
                calls.get(0);

        Client client =
                first.getClient();

        long totalCalls =
                calls.size();

        long answeredCalls =
                calls.stream()
                        .filter(this::isAnswered)
                        .count();

        long failedCalls =
                calls.stream()
                        .filter(this::isFailed)
                        .count();

        long notAnsweredCalls =
                totalCalls
                        - answeredCalls
                        - failedCalls;

        long totalSeconds =
                calls.stream()
                        .map(CallHistory::getDurationSeconds)
                        .filter(value -> value != null)
                        .mapToLong(Integer::longValue)
                        .sum();

        LocalDateTime firstCall =
                calls.stream()
                        .map(CallHistory::getCallTime)
                        .filter(value -> value != null)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

        LocalDateTime lastCall =
                calls.stream()
                        .map(CallHistory::getCallTime)
                        .filter(value -> value != null)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

        return new EmployeeClientCallReportResponse(

                client.getId(),

                client.getName(),

                client.getPhone(),

                totalCalls,

                answeredCalls,

                notAnsweredCalls,

                failedCalls,

                totalSeconds,

                totalSeconds / 60,

                firstCall,

                lastCall
        );
    }


    // =========================================================
    // GET EMPLOYEE CALLS
    // =========================================================

    private List<CallHistory> getCallsForEmployee(
            Long employeeId,
            LocalDate from,
            LocalDate to
    ) {

        if (from != null && to != null) {

            return callHistoryRepository
                    .findByUserIdAndCallTimeBetween(
                            employeeId,
                            from.atStartOfDay(),
                            to.atTime(LocalTime.MAX),
                            Pageable.unpaged()
                    )
                    .getContent();
        }

        return callHistoryRepository
                .findByUserId(
                        employeeId,
                        Pageable.unpaged()
                )
                .getContent();
    }


    // =========================================================
    // ANSWERED
    // =========================================================

    private boolean isAnswered(
            CallHistory call
    ) {

        if (call.getStatus() == null) {
            return false;
        }

        String status =
                call.getStatus()
                        .trim()
                        .toUpperCase();

        return status.equals("ANSWERED")
                || status.equals("COMPLETED")
                || status.equals("CONNECTED");
    }


    // =========================================================
    // FAILED
    // =========================================================

    private boolean isFailed(
            CallHistory call
    ) {

        if (call.getStatus() == null) {
            return false;
        }

        String status =
                call.getStatus()
                        .trim()
                        .toUpperCase();

        return status.equals("FAILED")
                || status.equals("ERROR");
    }


    // =========================================================
    // LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(
            Authentication authentication
    ) {

        if (authentication == null) {

            throw new RuntimeException(
                    "Authentication is required"
            );
        }

        String employeeCode =
                authentication.getName();

        return userRepository
                .findByEmployeeCode(
                        employeeCode
                )
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Logged-in employee not found: "
                                                + employeeCode
                                )
                );
    }


    // =========================================================
    // VALIDATE EMPLOYEE
    // =========================================================

    private User validateEmployee(
            Long employeeId
    ) {

        return userRepository
                .findById(employeeId)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Employee not found: "
                                                + employeeId
                                )
                );
    }


    // =========================================================
    // VALIDATE CLIENT
    // =========================================================

    private Client validateClient(
            Long clientId
    ) {

        return clientRepository
                .findById(clientId)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Client not found: "
                                                + clientId
                                )
                );
    }


    // =========================================================
    // TEAM VALIDATION
    // =========================================================

    private void validateTeamEmployee(
            User teamLead,
            Long employeeId
    ) {

        User employee =
                validateEmployee(
                        employeeId
                );

        if (teamLead.getTeam() == null) {

            throw new RuntimeException(
                    "Team Lead is not assigned to a team"
            );
        }

        if (employee.getTeam() == null) {

            throw new RuntimeException(
                    "Employee is not assigned to a team"
            );
        }

        if (!teamLead.getTeam()
                .getId()
                .equals(
                        employee.getTeam()
                                .getId()
                )) {

            throw new RuntimeException(
                    "You can only view employees from your team"
            );
        }
    }


    // =========================================================
    // PAGINATION
    // =========================================================

    private Pageable createPageable(
            int page,
            int size
    ) {

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 20;
        }

        if (size > 100) {
            size = 100;
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "callTime"
                )
        );
    }
}