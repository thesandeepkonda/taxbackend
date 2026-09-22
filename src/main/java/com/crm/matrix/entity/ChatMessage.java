package com.crm.matrix.entity;

import com.crm.matrix.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_sender", columnList = "sender_id"),
    @Index(name = "idx_chat_recipient", columnList = "recipient_id"),
    @Index(name = "idx_chat_group", columnList = "group_id"),
    @Index(name = "idx_chat_created", columnList = "created_at")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ChatGroup group;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType type;

    @Column(nullable = false)
    private Boolean isRead = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    @Column(name = "is_forwarded")
    private Boolean isForwarded = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;



    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "reaction", length = 50)
    private java.util.Map<Long, String> reactions = new java.util.HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER) // ---> ADD THIS ANNOTATION <---
    @CollectionTable(name = "chat_message_cleared", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id")
    private java.util.Set<Long> clearedBy = new java.util.HashSet<>();
    private Boolean isPinned = false;

}