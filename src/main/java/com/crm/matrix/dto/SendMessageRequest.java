package com.crm.matrix.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long recipientId;
    private Long groupId;
    private String content;
    private Long replyToId;
    private Boolean isForwarded;
}