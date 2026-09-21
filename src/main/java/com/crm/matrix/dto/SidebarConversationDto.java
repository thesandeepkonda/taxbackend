package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidebarConversationDto {
    private Long id;
    private String name;
    private String type; // "INDIVIDUAL" or "GROUP"

    private LocalDateTime updatedAt; // Used to float recent chats/groups to the top
    private long unreadCount;

    // Optional: Only populated when type is "INDIVIDUAL". Will be null for "GROUP".
    private Boolean isOnline;
}