package com.crm.matrix.service;

import com.crm.matrix.dto.AttendancePolicyResponse;
import com.crm.matrix.dto.CreateAttendancePolicyRequest;
import com.crm.matrix.entity.AttendancePolicy;
import com.crm.matrix.repository.AttendancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendancePolicyService {

    private final AttendancePolicyRepository attendancePolicyRepository;

    @Transactional
    public AttendancePolicyResponse createPolicy(CreateAttendancePolicyRequest request) {
        String name = request.getName().trim();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (attendancePolicyRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Attendance policy already exists: " + name);
        }

        AttendancePolicy policy = new AttendancePolicy();
        policy.setName(name);
        policy.setStartTime(startTime);
        policy.setEndTime(endTime);
        policy.setAllowedBreakMinutes(request.getAllowedBreakMinutes());
        policy.setWorkingDays(request.getWorkingDays().trim());
        policy.setActive(true);

        AttendancePolicy savedPolicy = attendancePolicyRepository.save(policy);
        return mapToResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public List<AttendancePolicyResponse> getAllPolicies() {
        return attendancePolicyRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendancePolicyResponse getPolicyById(Long id) {
        AttendancePolicy policy = attendancePolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + id));
        return mapToResponse(policy);
    }

    @Transactional
    public void deactivatePolicy(Long id) {
        AttendancePolicy policy = attendancePolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + id));
        policy.setActive(false);
        attendancePolicyRepository.save(policy);
    }

    @Transactional
    public void activatePolicy(Long id) {
        AttendancePolicy policy = attendancePolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attendance policy not found: " + id));
        policy.setActive(true);
        attendancePolicyRepository.save(policy);
    }

    private AttendancePolicyResponse mapToResponse(AttendancePolicy policy) {
        return AttendancePolicyResponse.builder()
                .attendancePolicyId(policy.getId())
                .name(policy.getName())
                .startTime(policy.getStartTime())
                .endTime(policy.getEndTime())
                .allowedBreakMinutes(policy.getAllowedBreakMinutes())
                .workingDays(policy.getWorkingDays())
                .active(policy.getActive())
                .build();
    }
    @Transactional(readOnly = true)
    public List<AttendancePolicyResponse> getActivePolicies() {
        return attendancePolicyRepository.findByActiveTrue().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendancePolicyResponse> getInactivePolicies() {
        return attendancePolicyRepository.findByActiveFalse().stream().map(this::mapToResponse).toList();
    }
}