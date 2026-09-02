package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_idle",
        indexes = {
                @Index(
                        name = "idx_attendance_idle_attendance",
                        columnList = "attendance_id"
                )
        }
)
@Getter
@Setter
public class AttendanceIdle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // ATTENDANCE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "attendance_id",
            nullable = false
    )
    private Attendance attendance;

    // =========================================================
    // IDLE START
    // =========================================================

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalDateTime startTime;

    // =========================================================
    // IDLE END
    // =========================================================

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // =========================================================
    // IDLE DURATION
    // =========================================================

    @Column(name = "duration_minutes")
    private Long durationMinutes;
}