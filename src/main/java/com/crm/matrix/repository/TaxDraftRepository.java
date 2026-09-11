package com.crm.matrix.repository;

import com.crm.matrix.entity.TaxDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaxDraftRepository extends JpaRepository<TaxDraft, Long> {
    List<TaxDraft> findByClientIdOrderByDraftVersionDesc(Long clientId);
    Optional<TaxDraft> findFirstByClientIdOrderByDraftVersionDesc(Long clientId);
}