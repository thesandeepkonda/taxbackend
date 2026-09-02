package com.crm.matrix.repository;

import com.crm.matrix.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role", "role.permissions", "department", "team"})
    Optional<User> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    List<User> findByTeamIdAndActiveTrue(Long teamId);

    @Query("""
        SELECT u FROM User u 
        JOIN FETCH u.role r 
        LEFT JOIN FETCH u.department d 
        LEFT JOIN FETCH u.team t 
        WHERE r.name = 'TEAM_LEAD' 
        AND u.active = true
    """)
    List<User> findAllTeamLeads();
}