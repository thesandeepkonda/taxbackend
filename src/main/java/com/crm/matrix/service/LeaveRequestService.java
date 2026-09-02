package com.crm.matrix.service;

import com.crm.matrix.dto.CreateLeaveRequest;
import com.crm.matrix.dto.LeaveRequestResponse;
import com.crm.matrix.entity.LeaveRequest;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.LeaveRequestStatus;
import com.crm.matrix.repository.LeaveRequestRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    private final UserRepository userRepository;


    // =========================================================
    // EMPLOYEE - CREATE LEAVE REQUEST
    // =========================================================

    @Transactional
    public LeaveRequestResponse createLeaveRequest(String employeeCode, CreateLeaveRequest request) {

        // -----------------------------------------------------
        // FIND EMPLOYEE FROM JWT
        // -----------------------------------------------------

        User employee = getActiveEmployee(employeeCode);


        LocalDate fromDate = request.getFromDate();

        LocalDate toDate = request.getToDate();


        // -----------------------------------------------------
        // DATE VALIDATION
        // -----------------------------------------------------

        if (toDate.isBefore(fromDate)) {

            throw new IllegalArgumentException("To date cannot be before from date");
        }


        // -----------------------------------------------------
        // CHECK PENDING OVERLAP
        // -----------------------------------------------------

        boolean pendingOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), LeaveRequestStatus.PENDING, toDate, fromDate);


        if (pendingOverlap) {

            throw new IllegalStateException("Employee already has a pending leave request for the selected dates");
        }


        // -----------------------------------------------------
        // CHECK APPROVED OVERLAP
        // -----------------------------------------------------

        boolean approvedOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), LeaveRequestStatus.APPROVED, toDate, fromDate);


        if (approvedOverlap) {

            throw new IllegalStateException("Employee already has approved leave for the selected dates");
        }


        // -----------------------------------------------------
        // CREATE LEAVE
        // -----------------------------------------------------

        LeaveRequest leaveRequest = new LeaveRequest();


        leaveRequest.setUser(employee);


        leaveRequest.setLeaveType(request.getLeaveType());


        leaveRequest.setFromDate(fromDate);


        leaveRequest.setToDate(toDate);


        leaveRequest.setDescription(request.getDescription());


        // -----------------------------------------------------
        // INITIAL STATUS
        // -----------------------------------------------------

        leaveRequest.setStatus(LeaveRequestStatus.PENDING);


        // -----------------------------------------------------
        // APPLICATION TIME
        // SERVER CONTROLLED
        // -----------------------------------------------------

        leaveRequest.setAppliedAt(LocalDateTime.now());


        // -----------------------------------------------------
        // ADMIN FIELDS
        // -----------------------------------------------------

        leaveRequest.setAdminRemark(null);

        leaveRequest.setProcessedBy(null);

        leaveRequest.setProcessedAt(null);


        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);


        return mapToResponse(saved);
    }


    // =========================================================
    // EMPLOYEE - GET MY REQUESTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaveRequests(String employeeCode) {

        User employee = getActiveEmployee(employeeCode);


        return leaveRequestRepository.findByUserIdOrderByFromDateDesc(employee.getId()).stream().map(this::mapToResponse).toList();
    }


    // =========================================================
    // ADMIN - GET PENDING REQUESTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingLeaveRequests() {

        return leaveRequestRepository.findByStatusOrderByFromDateAsc(LeaveRequestStatus.PENDING).stream().map(this::mapToResponse).toList();
    }


    // =========================================================
    // ADMIN - APPROVE
    // =========================================================

    @Transactional
    public LeaveRequestResponse approveLeave(Long leaveId, String adminEmployeeCode, String adminRemark) {

        User admin = getActiveEmployee(adminEmployeeCode);


        LeaveRequest leaveRequest = getLeaveRequest(leaveId);


        // -----------------------------------------------------
        // ONLY PENDING CAN BE APPROVED
        // -----------------------------------------------------

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {

            throw new IllegalStateException("Only pending leave requests can be approved");
        }


        // -----------------------------------------------------
        // CHECK APPROVED OVERLAP AGAIN
        // -----------------------------------------------------

        boolean approvedOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(leaveRequest.getUser().getId(),

                LeaveRequestStatus.APPROVED,

                leaveRequest.getToDate(),

                leaveRequest.getFromDate());


        if (approvedOverlap) {

            throw new IllegalStateException("Employee already has approved leave for the selected dates");
        }


        // -----------------------------------------------------
        // APPROVE
        // -----------------------------------------------------

        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);


        leaveRequest.setAdminRemark(adminRemark);


        leaveRequest.setProcessedBy(admin);


        leaveRequest.setProcessedAt(LocalDateTime.now());


        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);


        return mapToResponse(saved);
    }


    // =========================================================
    // ADMIN - REJECT
    // =========================================================

    @Transactional
    public LeaveRequestResponse rejectLeave(Long leaveId, String adminEmployeeCode, String adminRemark) {

        User admin = getActiveEmployee(adminEmployeeCode);


        LeaveRequest leaveRequest = getLeaveRequest(leaveId);


        // -----------------------------------------------------
        // ONLY PENDING CAN BE REJECTED
        // -----------------------------------------------------

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {

            throw new IllegalStateException("Only pending leave requests can be rejected");
        }


        // -----------------------------------------------------
        // REJECT
        // -----------------------------------------------------

        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);


        leaveRequest.setAdminRemark(adminRemark);


        leaveRequest.setProcessedBy(admin);


        leaveRequest.setProcessedAt(LocalDateTime.now());


        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);


        return mapToResponse(saved);
    }


    // =========================================================
    // GET LEAVE BY ID
    // =========================================================

    private LeaveRequest getLeaveRequest(Long leaveId) {

        return leaveRequestRepository.findById(leaveId).orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + leaveId));
    }


    // =========================================================
    // GET ACTIVE EMPLOYEE
    // =========================================================

    private User getActiveEmployee(String employeeCode) {

        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeCode));


        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new IllegalStateException("Employee is inactive");
        }


        return user;
    }


    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private LeaveRequestResponse mapToResponse(LeaveRequest leaveRequest) {

        User employee = leaveRequest.getUser();


        // -----------------------------------------------------
        // EMPLOYEE NAME
        // -----------------------------------------------------

        String employeeName = employee.getFirstName();


        if (employee.getLastName() != null && !employee.getLastName().isBlank()) {

            employeeName = employeeName + " " + employee.getLastName();
        }


        // -----------------------------------------------------
        // PROCESSED BY
        // -----------------------------------------------------

        String processedByName = null;


        if (leaveRequest.getProcessedBy() != null) {

            processedByName = leaveRequest.getProcessedBy().getFirstName();


            if (leaveRequest.getProcessedBy().getLastName() != null && !leaveRequest.getProcessedBy().getLastName().isBlank()) {

                processedByName = processedByName + " " + leaveRequest.getProcessedBy().getLastName();
            }
        }


        // -----------------------------------------------------
        // TOTAL DAYS
        // -----------------------------------------------------

        long totalDays = ChronoUnit.DAYS.between(leaveRequest.getFromDate(), leaveRequest.getToDate()) + 1;


        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        return LeaveRequestResponse.builder()

                .leaveId(leaveRequest.getId())

                .employeeCode(employee.getEmployeeCode())

                .employeeName(employeeName)

                .leaveType(leaveRequest.getLeaveType())

                .fromDate(leaveRequest.getFromDate())

                .toDate(leaveRequest.getToDate())

                .totalDays(totalDays)

                .description(leaveRequest.getDescription())

                .status(leaveRequest.getStatus())

                .appliedAt(leaveRequest.getAppliedAt())

                .adminRemark(leaveRequest.getAdminRemark())

                .processedByName(processedByName)

                .processedAt(leaveRequest.getProcessedAt())

                .build();
    }
}