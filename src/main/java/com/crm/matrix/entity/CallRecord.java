package com.crm.matrix.entity;

import com.crm.matrix.enums.CallStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "call_records",
        indexes = {
                @Index(name = "idx_call_client", columnList = "client_id"),
                @Index(name = "idx_call_employee", columnList = "employee_id"),
                @Index(name = "idx_call_status", columnList = "call_status"),
                @Index(name = "idx_call_started", columnList = "started_at")
        }
)
@Getter
@Setter
public class CallRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "provider_call_id")
    private String providerCallId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false)
    private CallStatus callStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "recording_url", length = 1000)
    private String recordingUrl;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}