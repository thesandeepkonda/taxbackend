package com.crm.matrix.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.crm.matrix.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCaseAndDepartmentId(
            String name,
            Long departmentId
    );

    @EntityGraph(attributePaths = {"department", "teamLead"})
    List<Team> findByDepartmentId(Long departmentId);
//
//    @EntityGraph(attributePaths = {"department", "teamLead"})
//    List<Team> findAll();
//
//    @EntityGraph(attributePaths = {"department", "teamLead"})
//    Optional<Team> findById(Long id);

    List<Team> findByActiveTrue();

    Page<Team> findByActive(boolean active, Pageable pageable);
}