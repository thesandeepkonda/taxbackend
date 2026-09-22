package com.crm.matrix.entity;

import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.EventTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events", indexes = {
        @Index(name = "idx_event_start_time", columnList = "start_time"),
        @Index(name = "idx_event_target", columnList = "target_type, target_id, target_department")
})
@Getter
@Setter
public class CalendarEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private EventTargetType targetType;

    @Column(name = "target_id")
    private Long targetId; // Used for UserId or TeamId. Null if targetType is ALL or DEPARTMENT.

    @Enumerated(EnumType.STRING)
    @Column(name = "target_department")
    private Department targetDepartment; // Used only when targetType is DEPARTMENT.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;
}