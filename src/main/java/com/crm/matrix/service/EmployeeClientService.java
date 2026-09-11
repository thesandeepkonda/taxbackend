package com.crm.matrix.service;


import com.crm.matrix.dto.AssignedClientResponseDto;
import com.crm.matrix.entity.ClientAssignment;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.ClientAssignmentRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeClientService {

    private final ClientAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;



    @Transactional(readOnly = true)
    public Page<AssignedClientResponseDto> getMyClients(
            Authentication authentication,
            Pageable pageable) {

        User employee =
                getLoggedInUser(authentication);

        return assignmentRepository
                .findByEmployeeIdAndActiveTrue(
                        employee.getId(),
                        pageable
                )
                .map(this::mapToDto);
    }


    // =========================================================
    // GET MY CLIENT BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public AssignedClientResponseDto getMyClient(
            Long clientId,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        ClientAssignment assignment =
                assignmentRepository
                        .findByClientIdAndEmployeeIdAndActiveTrue(
                                clientId,
                                employee.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client is not assigned to you"
                                )
                        );

        return mapToDto(assignment);
    }


    // =========================================================
    // MAPPING + MASKING
    // =========================================================

    private AssignedClientResponseDto mapToDto(
            ClientAssignment assignment) {

        var client = assignment.getClient();

        return AssignedClientResponseDto.builder()
                .clientId(client.getId())
                .name(client.getName())

                .phone(
                        maskPhone(
                                client.getPhone()
                        )
                )

                .email(
                        maskEmail(
                                client.getEmail()
                        )
                )

                .status(client.getStatus())

                .nextFollowUpAt(
                        client.getNextFollowUpAt()
                )

                .callInProgress(
                        assignment.getCallInProgress()
                )

                .lastCalledAt(
                        assignment.getLastCalledAt()
                )

                .build();
    }


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(
            Authentication authentication) {

        String employeeCode =
                authentication.getName();

        return userRepository
                .findByEmployeeCode(employeeCode)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"
                        )
                );
    }


    // =========================================================
    // MASK PHONE
    // =========================================================

    private String maskPhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return phone;
        }

        String digits =
                phone.replaceAll("[^0-9]", "");

        if (digits.length() <= 4) {
            return "****";
        }

        return digits.substring(0, 4)
                + "*".repeat(
                digits.length() - 4
        );
    }


    // =========================================================
    // MASK EMAIL
    // =========================================================

    private String maskEmail(String email) {

        if (email == null || email.isBlank()) {
            return email;
        }

        int atIndex =
                email.indexOf("@");

        if (atIndex <= 0) {
            return "****";
        }

        String username =
                email.substring(0, atIndex);

        String domain =
                email.substring(atIndex);

        if (username.length() <= 4) {
            return "****" + domain;
        }

        return "*".repeat(
                username.length() - 4
        )
                + username.substring(
                username.length() - 4
        )
                + domain;
    }
}