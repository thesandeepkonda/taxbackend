package com.crm.matrix.service;

import com.crm.matrix.dto.CallHistoryResponse;
import com.crm.matrix.dto.CallReportResponse;
import com.crm.matrix.dto.ClientCallReportResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallHippoAdminService {

    private final CallHistoryRepository callHistoryRepository;

    private final UserRepository userRepository;

    private final ClientRepository clientRepository;


    // =========================================================
    // EMPLOYEE CALL HISTORY
    // =========================================================

    public Page<CallHistoryResponse> getEmployeeCalls(
            String employeeCode,
            Long clientId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {

        User user =
                userRepository
                        .findByEmployeeCode(employeeCode)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found: "
                                                + employeeCode
                                )
                        );

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "callTime"
                        )
                );

        Page<CallHistory> calls;

        if (clientId != null) {

            calls =
                    callHistoryRepository
                            .findByUserIdAndClientId(
                                    user.getId(),
                                    clientId,
                                    pageable
                            );

        } else if (startDate != null &&
                endDate != null) {

            calls =
                    callHistoryRepository
                            .findByUserIdAndCallTimeBetween(
                                    user.getId(),
                                    startDate.atStartOfDay(),
                                    endDate.atTime(
                                            LocalTime.MAX
                                    ),
                                    pageable
                            );

        } else {

            calls =
                    callHistoryRepository
                            .findByUserId(
                                    user.getId(),
                                    pageable
                            );
        }

        return calls.map(
                CallHistoryResponse::from
        );
    }







}