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
    private final NotificationService notificationService;


    @Transactional
    public LeaveRequestResponse createLeaveRequest(String employeeCode, CreateLeaveRequest request) {

        User employee = getActiveEmployee(employeeCode);


        LocalDate fromDate = request.getFromDate();

        LocalDate toDate = request.getToDate();


        if (toDate.isBefore(fromDate)) {

            throw new IllegalArgumentException("To date cannot be before from date");
        }


        boolean pendingOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), LeaveRequestStatus.PENDING, toDate, fromDate);
        if (pendingOverlap) {

            throw new IllegalStateException("Employee already has a pending leave request for the selected dates");
        }
        boolean approvedOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), LeaveRequestStatus.APPROVED, toDate, fromDate);


        if (approvedOverlap) {

            throw new IllegalStateException("Employee already has approved leave for the selected dates");
        }
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(employee);
        leaveRequest.setLeaveType(request.getLeaveType());
        leaveRequest.setFromDate(fromDate);
        leaveRequest.setToDate(toDate);
        leaveRequest.setDescription(request.getDescription());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);
        leaveRequest.setAppliedAt(LocalDateTime.now());
        leaveRequest.setAdminRemark(null);
        leaveRequest.setProcessedBy(null);
        leaveRequest.setProcessedAt(null);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        String notifyTitle = "New Leave Request";
        String notifyMessage = employee.getFirstName() + " has requested " + request.getLeaveType() + " leave from " + fromDate + " to " + toDate + ".";

        // Notify all active Admins ONLY
        List<User> admins = userRepository.findByRoleNameAndActiveTrue("ADMIN");
        for (User admin : admins) {
            // Prevent self-notification if the person requesting IS an Admin
            if (admin.getId().equals(employee.getId())) {
                continue;
            }

            notificationService.sendNotification(
                    admin,
                    notifyTitle,
                    notifyMessage,
                    "LEAVE_REQUESTED",
                    "/admin/leave-approvals"
            );
        }
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaveRequests(String employeeCode) {

        User employee = getActiveEmployee(employeeCode);
        return leaveRequestRepository.findByUserIdOrderByFromDateDesc(employee.getId()).stream().map(this::mapToResponse).toList();
    }


    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingLeaveRequests() {

        return leaveRequestRepository.findByStatusOrderByFromDateAsc(LeaveRequestStatus.PENDING).stream().map(this::mapToResponse).toList();
    }


    @Transactional
    public LeaveRequestResponse approveLeave(Long leaveId, String adminEmployeeCode, String adminRemark) {

        User admin = getActiveEmployee(adminEmployeeCode);


        LeaveRequest leaveRequest = getLeaveRequest(leaveId);

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {

            throw new IllegalStateException("Only pending leave requests can be approved");
        }

        boolean approvedOverlap = leaveRequestRepository.existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(leaveRequest.getUser().getId(),

                LeaveRequestStatus.APPROVED,

                leaveRequest.getToDate(),

                leaveRequest.getFromDate());
        if (approvedOverlap) {
            throw new IllegalStateException("Employee already has approved leave for the selected dates");
        }
        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        leaveRequest.setAdminRemark(adminRemark);
        leaveRequest.setProcessedBy(admin);
        leaveRequest.setProcessedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        notificationService.sendNotification(
                leaveRequest.getUser(),
                "Leave Approved",
                "Your " + leaveRequest.getLeaveType() + " leave request from " + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate() + " has been approved.",
                "LEAVE_APPROVED",
                "/admin/leave-approvals"
        );
        return mapToResponse(saved);
    }


    @Transactional
    public LeaveRequestResponse rejectLeave(Long leaveId, String adminEmployeeCode, String adminRemark) {

        User admin = getActiveEmployee(adminEmployeeCode);


        LeaveRequest leaveRequest = getLeaveRequest(leaveId);

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {

            throw new IllegalStateException("Only pending leave requests can be rejected");
        }
        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);


        leaveRequest.setAdminRemark(adminRemark);


        leaveRequest.setProcessedBy(admin);


        leaveRequest.setProcessedAt(LocalDateTime.now());


        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        notificationService.sendNotification(
                leaveRequest.getUser(),
                "Leave Rejected",
                "Your " + leaveRequest.getLeaveType() + " leave request from " + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate() + " was rejected. Reason: " + adminRemark,
                "LEAVE_REJECTED","/leave-approvals"
        );


        return mapToResponse(saved);
    }


    private LeaveRequest getLeaveRequest(Long leaveId) {

        return leaveRequestRepository.findById(leaveId).orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + leaveId));
    }

    private User getActiveEmployee(String employeeCode) {

        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeCode));


        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new IllegalStateException("Employee is inactive");
        }
        return user;
    }


    private LeaveRequestResponse mapToResponse(LeaveRequest leaveRequest) {

        User employee = leaveRequest.getUser();
        String employeeName = employee.getFirstName();


        if (employee.getLastName() != null && !employee.getLastName().isBlank()) {

            employeeName = employeeName + " " + employee.getLastName();
        }

        String processedByName = null;


        if (leaveRequest.getProcessedBy() != null) {

            processedByName = leaveRequest.getProcessedBy().getFirstName();


            if (leaveRequest.getProcessedBy().getLastName() != null && !leaveRequest.getProcessedBy().getLastName().isBlank()) {

                processedByName = processedByName + " " + leaveRequest.getProcessedBy().getLastName();
            }
        }
        long totalDays = ChronoUnit.DAYS.between(leaveRequest.getFromDate(), leaveRequest.getToDate()) + 1;
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