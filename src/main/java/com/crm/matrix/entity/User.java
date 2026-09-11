package com.crm.matrix.entity;

import com.crm.matrix.enums.WorkMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_employee_code",
                        columnNames = "employee_code"
                ),
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "employee_code",
            nullable = false,
            length = 30
    )
    private String employeeCode;



    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;


    @Column(
            name = "last_name",
            length = 100
    )
    private String lastName;



    @Column(
            nullable = false,
            length = 150
    )
    private String email;


    @Column(
            nullable = false,
            length = 20
    )
    private String phone;


    @Column(
            nullable = false,
            length = 255
    )
    private String password;



    @Column(nullable = false)
    private Boolean active = true;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department_id",
            nullable = false
    )
    private Department department;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "attendance_policy_id",
            nullable = false
    )
    private AttendancePolicy attendancePolicy;




    @Enumerated(EnumType.STRING)
    @Column(
            name = "work_mode",
            nullable = false,
            length = 30
    )
    private WorkMode workMode;
    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY
    )
    private Set<LeaveRequest> leaveRequests = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY
    )
    private Set<Attendance> attendances = new HashSet<>();

    @Column(
            name = "callhippo_api_token",
            length = 1000
    )
    private String callHippoApiToken;

    @Column(
            name = "callhippo_from_number",
            length = 30
    )
    private String callHippoFromNumber;

    @Column(
            name = "callhippo_agent_id",
            length = 100
    )
    private String callHippoAgentId;
}