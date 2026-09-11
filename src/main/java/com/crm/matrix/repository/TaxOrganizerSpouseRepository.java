package com.crm.matrix.repository;

import com.crm.matrix.entity.TaxOrganizerSpouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxOrganizerSpouseRepository
        extends JpaRepository<TaxOrganizerSpouse, Long> {

    Optional<TaxOrganizerSpouse>
    findByTaxOrganizerId(Long taxOrganizerId);
}