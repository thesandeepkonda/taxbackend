package com.crm.matrix.service;

import com.crm.matrix.dto.ChatContactDto;
import com.crm.matrix.dto.SidebarConversationDto;
import com.crm.matrix.entity.*;
import com.crm.matrix.repository.*;
import com.crm.matrix.security.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSidebarService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    // NEW: Inject WebSocket template to push live badge counts
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<SidebarConversationDto> getSidebarConversations(Authentication authentication) {
        User currentUser = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long currentUserId = currentUser.getId();
        List<SidebarConversationDto> conversations = new ArrayList<>();

        // 1. Fetch Direct Individual Chats
        List<ChatMessage> directMessages = chatMessageRepository.findAllDirectMessagesForUser(currentUserId);
        Map<Long, ChatMessage> latestDm = new HashMap<>();

        for (ChatMessage msg : directMessages) {
            if (msg.getRecipient() == null || msg.getSender() == null) continue;

            Long partnerId = msg.getSender().getId().equals(currentUserId)
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();
            latestDm.putIfAbsent(partnerId, msg);
        }

        for (Map.Entry<Long, ChatMessage> entry : latestDm.entrySet()) {
            User partner = userRepository.findById(entry.getKey()).orElse(null);
            if (partner == null) continue;

            ChatMessage msg = entry.getValue();
            String partnerName = partner.getFirstName() + (partner.getLastName() != null ? " " + partner.getLastName() : "");
            boolean isOnline = WebSocketEventListener.isUserOnline(partner.getEmployeeCode());

            // ---> GET DIRECT MESSAGE UNREAD COUNT <---
            int unreadCount = chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(partner.getId(), currentUserId);

            conversations.add(SidebarConversationDto.builder()
                    .id(partner.getId())
                    .name(partnerName)
                    .type("INDIVIDUAL")
                    .updatedAt(msg.getCreatedAt())
                    .unreadCount(unreadCount)
                    .isOnline(isOnline)
                    .build());
        }

        // 2. Fetch Group Chats
        List<ChatGroupMember> userMemberships = groupMemberRepository.findByUserId(currentUserId);
        for (ChatGroupMember membership : userMemberships) {
            ChatGroup group = membership.getGroup();
            ChatMessage latestGroupMsg = chatMessageRepository.findTopByGroupIdOrderByCreatedAtDesc(group.getId()).orElse(null);

            LocalDateTime groupActivityTime = latestGroupMsg != null
                    ? latestGroupMsg.getCreatedAt()
                    : group.getCreatedAt();

            // ---> GET GROUP MESSAGE UNREAD COUNT (High Water Mark) <---
            int unreadGroupCount = chatMessageRepository.countUnreadGroupMessages(group.getId(), membership.getLastReadAt());

            conversations.add(SidebarConversationDto.builder()
                    .id(group.getId())
                    .name(group.getName())
                    .type("GROUP")
                    .updatedAt(groupActivityTime)
                    .unreadCount(unreadGroupCount)
                    .isOnline(null)
                    .build());
        }

        // 3. Sort descending by updatedAt
        conversations.sort((a, b) -> {
            LocalDateTime timeA = a.getUpdatedAt() != null ? a.getUpdatedAt() : LocalDateTime.MIN;
            LocalDateTime timeB = b.getUpdatedAt() != null ? b.getUpdatedAt() : LocalDateTime.MIN;
            return timeB.compareTo(timeA);
        });

        return conversations;
    }

    @Transactional(readOnly = true)
    public List<ChatContactDto> getAvailableContacts(Authentication authentication) {
        User currentUser = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long currentUserId = currentUser.getId();

        List<ChatMessage> directMessages = chatMessageRepository.findAllDirectMessagesForUser(currentUserId);
        Set<Long> existingChatPartnerIds = new HashSet<>();

        for (ChatMessage msg : directMessages) {
            if (msg.getRecipient() == null || msg.getSender() == null) continue;

            Long partnerId = msg.getSender().getId().equals(currentUserId)
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();
            existingChatPartnerIds.add(partnerId);
        }

        List<User> allActiveUsers = userRepository.findByActiveTrue();

        return allActiveUsers.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> !existingChatPartnerIds.contains(user.getId()))
                .map(user -> ChatContactDto.builder()
                        .id(user.getId())
                        .name(user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : ""))
                        .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : "")
                        .roleName(user.getRole() != null ? user.getRole().getName() : "")
                        .isOnline(WebSocketEventListener.isUserOnline(user.getEmployeeCode()))
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================
    // NEW: CALCULATE TOTAL UNREAD COUNT
    // =========================================================
    @Transactional(readOnly = true)
    public int getTotalUnreadCount(User currentUser) {
        Long currentUserId = currentUser.getId();
        int totalUnread = 0;

        // 1. Direct Messages - Done in a single query now
        totalUnread += chatMessageRepository.countByRecipientIdAndIsReadFalse(currentUserId);

        // 2. Group Messages
        List<ChatGroupMember> userMemberships = groupMemberRepository.findByUserId(currentUserId);
        for (ChatGroupMember membership : userMemberships) {
            totalUnread += chatMessageRepository.countUnreadGroupMessages(membership.getGroup().getId(), membership.getLastReadAt());
        }

        return totalUnread;
    }
    // =========================================================
    // NEW: BROADCAST TO WEBSOCKET
    // =========================================================
    @Transactional(readOnly = true)
    public void broadcastLiveUnreadCount(User targetUser) {
        int unreadCount = getTotalUnreadCount(targetUser);

        messagingTemplate.convertAndSendToUser(
                targetUser.getEmployeeCode(),
                "/queue/unread-count",
                Map.of("unreadCount", unreadCount)
        );
    }
}