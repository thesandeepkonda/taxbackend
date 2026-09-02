package com.crm.matrix.repository;

import com.crm.matrix.entity.ClientDocument;
import com.crm.matrix.entity.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientDocumentRepository
        extends JpaRepository<ClientDocument, Long> {

    List<ClientDocument>
    findByRequestOrderByCreatedAtDesc(
            DocumentRequest request
    );

    Optional<ClientDocument>
    findByIdAndClient(
            Long id,
            com.crm.matrix.entity.Client client
    );
}