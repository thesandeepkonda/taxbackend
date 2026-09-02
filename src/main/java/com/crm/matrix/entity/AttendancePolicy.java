package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "attendance_policies", uniqueConstraints = {@UniqueConstraint(name = "uk_attendance_policy_name", columnNames = "name")})
@Getter
@Setter
public class AttendancePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;


    // The new flexible daily allowance
    @Column(name = "allowed_break_minutes", nullable = false)
    private Integer allowedBreakMinutes;

    @Column(nullable = false)
    private Boolean active = true;
}