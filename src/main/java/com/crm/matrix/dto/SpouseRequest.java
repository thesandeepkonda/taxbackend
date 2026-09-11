package com.crm.matrix.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpouseRequest {

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