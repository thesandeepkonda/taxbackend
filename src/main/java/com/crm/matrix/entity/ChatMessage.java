package com.crm.matrix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_sender", columnList = "sender_id"),
    @Index(name = "idx_chat_recipient", columnList = "recipient_id"),
    @Index(name = "idx_chat_type", columnList = "message_type")
})
@Getter
@Setter
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Can store the text message, or act as a "caption" if an image is sent
    @Column(columnDefinition = "TEXT")
    private String content;

    // e.g., TEXT, IMAGE, DOCUMENT, AUDIO
    @Column(name = "message_type", nullable = false, length = 30)
    private String messageType = "TEXT"; 

    // The path/URL to the uploaded file
    @Column(name = "file_url", length = 1000)
    private String fileUrl; 

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
}