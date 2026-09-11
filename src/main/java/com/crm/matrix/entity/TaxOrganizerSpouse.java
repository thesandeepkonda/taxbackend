package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tax_organizer_spouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxOrganizerSpouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tax_organizer_id",
            nullable = false,
            unique = true
    )
    private TaxOrganizer taxOrganizer;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "ssn_itin", nullable = false)
    private String ssnItin;

    @Column(name = "visa_status", nullable = false)
    private String visaStatus;

    @Column(name = "visa_status_changed", nullable = false)
    private Boolean visaStatusChanged;

    @Column(name = "occupation", nullable = false)
    private String occupation;

    private String email;

    @Column(name = "has_ssn")
    private Boolean hasSsn;

    @Column(name = "has_itin")
    private Boolean hasItin;
}