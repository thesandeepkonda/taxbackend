package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_request_items")
@Getter
@Setter
public class DocumentRequestItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private DocumentRequest request;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean required = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean uploaded = false;
}