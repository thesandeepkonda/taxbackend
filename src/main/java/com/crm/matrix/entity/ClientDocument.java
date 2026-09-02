package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "client_documents",
        indexes = {
                @Index(name = "idx_document_request", columnList = "request_id"),
                @Index(name = "idx_document_client", columnList = "client_id")
        }
)
@Getter
@Setter
public class ClientDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Document request
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private DocumentRequest request;

    // Client
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Example: PAN, Aadhaar, GST Certificate
    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false)
    private Boolean uploaded = true;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "remarks", length = 500)
    private String remarks;
}