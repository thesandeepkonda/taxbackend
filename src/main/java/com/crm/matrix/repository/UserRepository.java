package com.crm.matrix.repository;

import com.crm.matrix.entity.User;
import com.crm.matrix.enums.Department;
import com.crm.matrix.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ FIX: Added "permissions" to attributePaths
    @EntityGraph(attributePaths = {"team", "permissions"})
    Optional<User> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    List<User> findByTeamIdAndActiveTrue(Long teamId);

    @Query("""
        SELECT u FROM User u 
        LEFT JOIN FETCH u.team t 
        WHERE u.role = com.crm.matrix.enums.Role.TEAM_LEAD 
        AND u.active = true
    """)
    List<User> findAllTeamLeads();

    Page<User> findByActive(boolean active, Pageable pageable);

    List<User> findByDepartmentAndActiveTrue(Department department);

    List<User> findByActiveTrue();

    List<User> findByTeamId(Long teamId);

    // ✅ FIX: Added "permissions" here as well if you use email for login anywhere
    @EntityGraph(attributePaths = {"team", "permissions"})
    Optional<User> findByEmail(String email);

    Optional<User> findByCallHippoAgentId(String callHippoAgentId);

    long countByActiveTrue();

    long countByTeamIdAndActiveTrue(Long teamId);

    List<User> findByRoleAndActiveTrue(Role role);
}