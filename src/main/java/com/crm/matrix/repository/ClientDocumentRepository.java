package com.crm.matrix.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.ClientDocument;
import com.crm.matrix.entity.DocumentRequest;
import com.crm.matrix.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClientDocumentRepository
        extends JpaRepository<ClientDocument, Long> {

    // =========================================================
    // CLIENT DOCUMENTS - PAGINATED
    // =========================================================

    Page<ClientDocument> findByClientIdOrderByCreatedAtDesc(
            Long clientId,
            Pageable pageable
    );

    List<ClientDocument>
    findByClientIdOrderByCreatedAtDesc(
            Long clientId
    );

    List<ClientDocument> findByClientIdOrderByUpdatedAtDesc(
            Long clientId
    );

    Optional<ClientDocument> findById(Long documentId);

    List<ClientDocument> findByStatusOrderByUploadedAtDesc(
            DocumentStatus status
    );
    List<ClientDocument> findByClientIdAndStatus(
            Long clientId,
            DocumentStatus status
    );

    Page<ClientDocument> findByClientIdAndStatus(Long clientId, DocumentStatus status, Pageable pageable);


    // =========================================================
    // COUNTS
    // =========================================================

    long countByClientId(Long clientId);

    long countByClientIdAndStatus(
            Long clientId,
            DocumentStatus status
    );

    long countByStatus(
            DocumentStatus status
    );


    // =========================================================
    // PAGINATED SUBMITTED CLIENTS
    // =========================================================

    @Query("""
        SELECT c
        FROM Client c
        WHERE EXISTS (
            SELECT d.id
            FROM ClientDocument d
            WHERE d.client.id = c.id
        )
        AND NOT EXISTS (
            SELECT d2.id
            FROM ClientDocument d2
            WHERE d2.client.id = c.id
            AND (
                d2.status = com.crm.matrix.enums.DocumentStatus.PENDING
                OR
                d2.status = com.crm.matrix.enums.DocumentStatus.REJECTED
            )
        )
        ORDER BY c.createdAt DESC
    """)
    Page<Client> findClientsWithSubmittedDocuments(
            Pageable pageable
    );


    // =========================================================
    // PAGINATED PENDING CLIENTS
    // =========================================================

    @Query("""
        SELECT c
        FROM Client c
        WHERE EXISTS (
            SELECT d.id
            FROM ClientDocument d
            WHERE d.client.id = c.id
            AND (
                d.status = com.crm.matrix.enums.DocumentStatus.PENDING
                OR
                d.status = com.crm.matrix.enums.DocumentStatus.REJECTED
            )
        )
        ORDER BY c.createdAt DESC
    """)
    Page<Client> findClientsWithPendingDocuments(
            Pageable pageable
    );

    List<ClientDocument>
    findByRequestOrderByCreatedAtDesc(
            DocumentRequest request
    );

    List<ClientDocument>findByClientIdOrderByUploadedAtDesc(Long id);

    List<ClientDocument> findByRequestOrderByUpdatedAtDesc(
            DocumentRequest request
    );

    @Query("SELECT c FROM Client c WHERE " +
            "(SELECT COUNT(d) FROM ClientDocument d WHERE d.client = c) > 0 " +
            "AND " +
            "(SELECT COUNT(d) FROM ClientDocument d WHERE d.client = c AND d.status != com.crm.matrix.enums.DocumentStatus.VERIFIED) = 0")
    Page<Client> findClientsWithAllDocumentsVerified(Pageable pageable);
    // ClientDocumentRepository interface లోపల:
    @Query("SELECT DISTINCT d.client FROM ClientDocument d WHERE d.status = com.crm.matrix.enums.DocumentStatus.VERIFIED")
    Page<Client> findClientsWithVerifiedDocuments(Pageable pageable);
}