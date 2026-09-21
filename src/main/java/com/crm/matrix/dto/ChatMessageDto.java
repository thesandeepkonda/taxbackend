package com.crm.matrix.dto;

import com.crm.matrix.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private Long groupId;
    private String content;
    private String fileUrl;
    private String fileName;
    private MessageType type;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private Long replyToId;
    private Boolean isForwarded;
    private Boolean isDeleted;
    private ChatMessageDto replyToMessage;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private java.util.Map<Long, String> reactions;
    private Boolean isPinned;
}