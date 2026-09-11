package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_drafts", indexes = {
        @Index(name = "idx_taxdraft_client", columnList = "client_id")
})
@Getter
@Setter
public class TaxDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prep_employee_id", nullable = false)
    private User prepEmployee;

    @Column(nullable = false)
    private Integer draftVersion = 1; // 1 for Draft 1, 2 for Draft 2, etc.

    // --- File Storage Fields ---
    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "prep_remarks", length = 2000)
    private String prepRemarks; // Notes from the prep team

    @Column(name = "admin_feedback", length = 2000)
    private String adminFeedback; // Why the admin rejected it

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
}