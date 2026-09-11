package com.crm.matrix.repository;

import com.crm.matrix.entity.TaxOrganizerDependent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxOrganizerDependentRepository
        extends JpaRepository<TaxOrganizerDependent, Long> {

    List<TaxOrganizerDependent>
    findByTaxOrganizerIdOrderByIdAsc(Long taxOrganizerId);
}