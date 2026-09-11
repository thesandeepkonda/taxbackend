package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String content;
    private String messageType; // TEXT, IMAGE, DOCUMENT
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Boolean isRead;
    private LocalDateTime createdAt;
}