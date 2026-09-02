
        package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

        @Entity
@Table(
        name = "client_comments",
        indexes = {
                @Index(
                        name = "idx_comment_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_comment_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_comment_assignment",
                        columnList = "assignment_id"
                ),
                @Index(
                        name = "idx_comment_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
public class ClientComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Client
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false
    )
    private Client client;

    // Employee who added the comment
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private User employee;

    // Assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assignment_id"
    )
    private ClientAssignment assignment;

    // Comment
    @Column(
            name = "comment",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String comment;

    // Comment type
    @Column(
            name = "comment_type",
            length = 30
    )
    private String commentType = "GENERAL";

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

            @ManyToOne(fetch = FetchType.LAZY)
            @JoinColumn(name = "deleted_by")
            private User deletedBy;
}

