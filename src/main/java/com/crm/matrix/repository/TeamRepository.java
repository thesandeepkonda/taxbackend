package com.crm.matrix.repository;

import com.crm.matrix.entity.Team;
import com.crm.matrix.enums.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"teamLead"})
    Optional<Team> findById(Long id);

    @EntityGraph(attributePaths = {"teamLead"})
    List<Team> findAll();

    List<Team> findByActiveTrue();

    Page<Team> findByActive(boolean active, Pageable pageable);

    // ✅ Replaced ID-based check with Enum-based check
    boolean existsByNameIgnoreCaseAndDepartment(String name, Department department);

    // ✅ Replaced ID-based fetch with Enum-based fetch
    @EntityGraph(attributePaths = {"teamLead"})
    List<Team> findByDepartment(Department department);

    List<Team> findByTeamLeadId(Long id);
}