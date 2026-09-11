package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "call_history",
        indexes = {

                @Index(
                        name = "idx_call_history_client",
                        columnList = "client_id"
                ),

                @Index(
                        name = "idx_call_history_user",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_call_history_call_sid",
                        columnList = "call_sid"
                ),

                @Index(
                        name = "idx_call_history_to_number",
                        columnList = "to_number"
                )
        }
)
@Getter
@Setter
public class CallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "client_id",
            nullable = false
    )
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "call_sid",
            unique = true,
            length = 255
    )
    private String callSid;

    @Column(
            name = "from_number",
            length = 30
    )
    private String fromNumber;

    @Column(
            name = "to_number",
            length = 30
    )
    private String toNumber;

    @Column(
            name = "agent_id",
            length = 100
    )
    private String agentId;

    @Column(
            name = "call_type",
            length = 50
    )
    private String callType;

    @Column(
            name = "status",
            length = 50
    )
    private String status;

    @Column(
            name = "duration",
            length = 20
    )
    private String duration;

    @Column(
            name = "duration_seconds"
    )
    private Integer durationSeconds;

    @Column(
            name = "recording_url",
            length = 2000
    )
    private String recordingUrl;

    @Column(
            name = "hangup_by",
            length = 50
    )
    private String hangupBy;

    @Column(
            name = "answered_device",
            length = 50
    )
    private String answeredDevice;

    @Column(name = "billed_minutes")
    private Double billedMinutes;

    @Column(
            name = "call_charge",
            length = 50
    )
    private String callCharge;

    @Column(
            name = "country_name",
            length = 100
    )
    private String countryName;

    @Column(
            name = "call_time"
    )
    private LocalDateTime callTime;

    @Column(
            name = "start_time"
    )
    private LocalDateTime startTime;

    @Column(
            name = "end_time"
    )
    private LocalDateTime endTime;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "INITIATED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}