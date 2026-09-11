package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tax_organizer_dependents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxOrganizerDependent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_organizer_id", nullable = false)
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

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "months_lived_with_you", nullable = false)
    private Integer monthsLivedWithYou;

    @Column(name = "us_citizen_resident", nullable = false)
    private Boolean usCitizenResident;

    @Column(name = "child_care_expenses", nullable = false)
    private Boolean childCareExpenses;

    @Column(name = "ssn_status")
    private String ssnStatus;

    @Column(name = "itin_application_required")
    private Boolean itinApplicationRequired;
}