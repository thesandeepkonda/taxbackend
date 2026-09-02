package com.crm.matrix.entity;

import com.crm.matrix.enums.LeaveRequestStatus;
import com.crm.matrix.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "leave_requests",
        indexes = {
                @Index(
                        name = "idx_leave_request_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_leave_request_dates",
                        columnList = "from_date,to_date"
                ),
                @Index(
                        name = "idx_leave_request_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
public class LeaveRequest extends BaseEntity {

    // =========================================================
    // LEAVE ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    // =========================================================
    // LEAVE TYPE
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "leave_type",
            nullable = false,
            length = 30
    )
    private LeaveType leaveType;


    // =========================================================
    // FROM DATE
    // =========================================================

    @Column(
            name = "from_date",
            nullable = false
    )
    private LocalDate fromDate;


    // =========================================================
    // TO DATE
    // =========================================================

    @Column(
            name = "to_date",
            nullable = false
    )
    private LocalDate toDate;


    // =========================================================
    // DESCRIPTION / REASON
    // =========================================================

    @Column(
            name = "description",
            length = 1000
    )
    private String description;


    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private LeaveRequestStatus status;


    // =========================================================
    // APPLIED AT
    // =========================================================

    @Column(
            name = "applied_at",
            nullable = false
    )
    private LocalDateTime appliedAt;


    // =========================================================
    // ADMIN REMARK
    // =========================================================

    @Column(
            name = "admin_remark",
            length = 1000
    )
    private String adminRemark;


    // =========================================================
    // PROCESSED BY
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "processed_by"
    )
    private User processedBy;


    // =========================================================
    // PROCESSED AT
    // =========================================================

    @Column(
            name = "processed_at"
    )
    private LocalDateTime processedAt;
}