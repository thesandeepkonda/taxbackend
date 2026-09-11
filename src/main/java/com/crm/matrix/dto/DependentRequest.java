package com.crm.matrix.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependentRequest {

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