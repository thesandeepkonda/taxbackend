package com.crm.matrix.dto;

import com.crm.matrix.enums.TaxOrganizerStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxOrganizerResponse {

    private Long id;

    // =========================================================
    // CLIENT
    // =========================================================

    private Long clientId;
    private String clientName;

    // =========================================================
    // EMPLOYEE
    // =========================================================

    private Long employeeId;
    private String employeeName;

    // =========================================================
    // PERSONAL INFORMATION
    // =========================================================

    private String firstName;
    private String middleName;
    private String lastName;

    private LocalDate dateOfBirth;

    private String ssnItin;

    private String visaStatus;
    private Boolean visaStatusChanged;

    private String maritalStatus;

    private String currentAddress;
    private String emailAddress;
    private String phoneNumber;
    private String occupation;

    private String statesLivedIn2026;

    // =========================================================
    // DEPENDENTS
    // =========================================================

    private Boolean hasDependents;

    private SpouseResponse spouse;

    private List<DependentResponse> dependents;

    // =========================================================
    // FILE
    // =========================================================

    private String fileName;

    private String viewUrl;

    private String downloadUrl;

    // =========================================================
    // STATUS
    // =========================================================

    private TaxOrganizerStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;


    // =========================================================
    // SPOUSE
    // =========================================================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpouseResponse {

        private Long id;

        private String firstName;
        private String middleName;
        private String lastName;

        private LocalDate dateOfBirth;

        private String ssnItin;

        private String visaStatus;
        private Boolean visaStatusChanged;

        private String occupation;
        private String email;

        private Boolean hasSsn;
        private Boolean hasItin;
    }


    // =========================================================
    // DEPENDENT
    // =========================================================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DependentResponse {

        private Long id;

        private String firstName;
        private String middleName;
        private String lastName;

        private LocalDate dateOfBirth;

        private String ssnItin;

        private String relationship;

        private Integer monthsLivedWithYou;

        private Boolean usCitizenResident;

        private Boolean childCareExpenses;

        private String ssnStatus;

        private Boolean itinApplicationRequired;
    }
}