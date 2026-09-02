package com.crm.matrix.repository;

import com.crm.matrix.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadedDocumentRepository
        extends JpaRepository<UploadedDocument, Long> {

    List<UploadedDocument>
    findByRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            Long requestId
    );

    List<UploadedDocument>
    findByClientIdAndDeletedFalseOrderByCreatedAtDesc(
            Long clientId
    );
}