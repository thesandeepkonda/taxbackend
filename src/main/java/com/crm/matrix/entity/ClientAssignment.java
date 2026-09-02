package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "client_assignments",
        indexes = {
                @Index(name = "idx_assignment_client", columnList = "client_id"),
                @Index(name = "idx_assignment_employee", columnList = "employee_id"),
                @Index(name = "idx_assignment_active", columnList = "active")
        }
)
@Getter
@Setter
public class ClientAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false
    )
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "assigned_by",
            nullable = false
    )
    private User assignedBy;

    @Column(
            name = "assigned_at",
            nullable = false
    )
    private LocalDateTime assignedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(
            name = "assignment_reason",
            length = 500
    )
    private String assignmentReason;

    @Column(
            name = "call_in_progress",
            nullable = false
    )
    private Boolean callInProgress = false;

    @Column(name = "last_called_at")
    private LocalDateTime lastCalledAt;
}