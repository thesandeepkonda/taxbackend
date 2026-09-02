package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(         
    name = "activity_logs",
    indexes = {
        @Index(name = "idx_activity_target_user", columnList = "target_user_id")
    }
)
@Getter
@Setter
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;
}