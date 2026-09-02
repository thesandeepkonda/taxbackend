package com.crm.matrix.entity;

import com.crm.matrix.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_user_date",
                        columnNames = {
                                "user_id",
                                "attendance_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_attendance_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_attendance_date",
                        columnList = "attendance_date"
                ),
                @Index(
                        name = "idx_attendance_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    // =========================================================
    // ATTENDANCE DATE
    // =========================================================

    @Column(
            name = "attendance_date",
            nullable = false
    )
    private LocalDate attendanceDate;


    // =========================================================
    // CHECK IN
    // =========================================================

    @Column(name = "check_in")
    private LocalDateTime checkIn;


    // =========================================================
    // CHECK OUT
    // =========================================================

    @Column(name = "check_out")
    private LocalDateTime checkOut;


    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private AttendanceStatus status;


    // =========================================================
    // TOTAL WORKING MINUTES
    // =========================================================

    @Column(
            name = "total_work_minutes",
            nullable = false
    )
    private Long totalWorkMinutes = 0L;


    // =========================================================
    // TOTAL BREAK MINUTES
    // =========================================================

    @Column(
            name = "total_break_minutes",
            nullable = false
    )
    private Long totalBreakMinutes = 0L;


    // =========================================================
    // TOTAL IDLE MINUTES
    // =========================================================

    @Column(
            name = "total_idle_minutes",
            nullable = false
    )
    private Long totalIdleMinutes = 0L;

    @OneToMany(
            mappedBy = "attendance",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AttendanceBreak> breaks =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "attendance",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AttendanceIdle> idlePeriods =
            new ArrayList<>();
}