package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_breaks",
        indexes = {
                @Index(
                        name = "idx_attendance_break_attendance",
                        columnList = "attendance_id"
                ),
                @Index(
                        name = "idx_attendance_break_start",
                        columnList = "start_time"
                )
        }
)
@Getter
@Setter
public class AttendanceBreak extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attendance_id",
            nullable = false
    )
    private Attendance attendance;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private Long durationMinutes;
}