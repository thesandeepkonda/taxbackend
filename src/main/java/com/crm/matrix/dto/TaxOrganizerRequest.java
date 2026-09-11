package com.crm.matrix.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxOrganizerRequest {

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

    private Boolean hasDependents;

    private SpouseRequest spouse;

    private List<DependentRequest> dependents;
}