package com.crm.matrix.repository;

import com.crm.matrix.entity.TaxOrganizer;
import com.crm.matrix.enums.TaxOrganizerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxOrganizerRepository
        extends JpaRepository<TaxOrganizer, Long> {

    Optional<TaxOrganizer>
    findByClientId(Long clientId);

    Optional<TaxOrganizer>
    findByClientIdAndStatus(
            Long clientId,
            TaxOrganizerStatus status
    );
}