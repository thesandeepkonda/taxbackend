package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "call_logs",
        indexes = {
                @Index(name = "idx_call_client", columnList = "client_id"),
                @Index(name = "idx_call_employee", columnList = "employee_id"),
                @Index(name = "idx_call_assignment", columnList = "assignment_id"),
                @Index(name = "idx_call_start_time", columnList = "start_time"),
                @Index(name = "idx_call_provider_id", columnList = "provider_call_id")
        }
)
@Getter
@Setter
public class CallLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // CLIENT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false
    )
    private Client client;


    // =========================================================
    // EMPLOYEE WHO MADE THE CALL
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private User employee;


    // =========================================================
    // CLIENT ASSIGNMENT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "assignment_id",
            nullable = false
    )
    private ClientAssignment assignment;


    // =========================================================
    // CALLHIPPO / EXTERNAL CALL ID
    // =========================================================

    @Column(
            name = "provider_call_id",
            length = 255
    )
    private String providerCallId;


    // =========================================================
    // CALL START TIME
    // =========================================================

    @Column(name = "start_time")
    private LocalDateTime startTime;


    // =========================================================
    // CALL END TIME
    // =========================================================

    @Column(name = "end_time")
    private LocalDateTime endTime;


    // =========================================================
    // WAS CALL ANSWERED?
    // =========================================================

    @Column(
            name = "answered",
            nullable = false
    )
    private Boolean answered = false;


    // =========================================================
    // CALL DURATION
    // =========================================================

    /**
     * Duration in seconds.
     *
     * Example:
     * 125 = 2 minutes 5 seconds
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;


    // =========================================================
    // CALL RECORDING
    // =========================================================

    @Column(
            name = "recording_url",
            length = 1000
    )
    private String recordingUrl;


    // =========================================================
    // OPTIONAL CALL STATUS
    // =========================================================

    @Column(
            name = "call_status",
            length = 30
    )
    private String callStatus;


    // =========================================================
    // OPTIONAL FAILURE REASON
    // =========================================================

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;
}