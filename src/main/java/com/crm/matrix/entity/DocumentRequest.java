package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "document_requests",
        indexes = {
                @Index(name = "idx_doc_request_client", columnList = "client_id"),
                @Index(name = "idx_doc_request_employee", columnList = "employee_id"),
                @Index(name = "idx_doc_request_token", columnList = "share_token")
        }
)
@Getter
@Setter
public class DocumentRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Client
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // DOC employee
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    // Random public token
    @Column(name = "share_token", nullable = false, unique = true, length = 100)
    private String shareToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "submitted", nullable = false)
    private Boolean submitted = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    public void generateToken() {
        if (shareToken == null) {
            shareToken = UUID.randomUUID().toString()
                    .replace("-", "");
        }
    }
}