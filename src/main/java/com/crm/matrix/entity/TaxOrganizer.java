package com.crm.matrix.entity;

import com.crm.matrix.enums.TaxOrganizerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_organizers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxOrganizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // CLIENT
    // =========================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    // =========================================================
    // EMPLOYEE WHO FILLED THE FORM
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    // =========================================================
    // PERSONAL INFORMATION
    // =========================================================

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

    @Column(name = "marital_status", nullable = false)
    private String maritalStatus;

    @Column(name = "current_address", nullable = false)
    private String currentAddress;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "occupation", nullable = false)
    private String occupation;

    @Column(name = "states_lived_in_2026", columnDefinition = "TEXT")
    private String statesLivedIn2026;

    // =========================================================
    // DEPENDENTS
    // =========================================================

    @Column(name = "has_dependents")
    private Boolean hasDependents;

    // =========================================================
    // FILE
    // =========================================================

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxOrganizerStatus status;

    // =========================================================
    // TIMESTAMPS
    // =========================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}