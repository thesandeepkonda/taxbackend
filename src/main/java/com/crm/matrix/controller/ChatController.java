package com.crm.matrix.controller;

import com.crm.matrix.dto.ChatMessageDto;
import com.crm.matrix.dto.GroupMemberDto;
import com.crm.matrix.dto.GroupMembersResponseDto;
import com.crm.matrix.entity.ChatGroup;
import com.crm.matrix.entity.ChatGroupMember;
import com.crm.matrix.entity.ChatMessage;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.MessageType;
import com.crm.matrix.repository.ChatGroupMemberRepository;
import com.crm.matrix.repository.ChatGroupRepository;
import com.crm.matrix.repository.ChatMessageRepository;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.security.CustomUserDetails;
import com.crm.matrix.service.ChatService;
import com.crm.matrix.service.ChatSidebarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final UserRepository userRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final ChatService chatService;
    private final ChatSidebarService chatSidebarService;

    @Transactional
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessageDto dto, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("Message rejected: Unauthenticated session.");
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User sender = userDetails.getUser();

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setContent(dto.getContent());
        message.setFileUrl(dto.getFileUrl());
        message.setFileName(dto.getFileName());
        message.setType(dto.getType() != null ? dto.getType() : MessageType.TEXT);
        message.setIsRead(false);
        message.setIsDeleted(false);
        message.setIsEdited(false);

        // Map Forward and Reply metadata
        message.setIsForwarded(Boolean.TRUE.equals(dto.getIsForwarded()));
        if (dto.getReplyToId() != null) {
            chatMessageRepository.findById(dto.getReplyToId()).ifPresent(message::setReplyTo);
        }

        if (dto.getGroupId() != null) {
            ChatGroup group = chatGroupRepository.findById(dto.getGroupId()).orElseThrow(() -> new RuntimeException("Group not found"));
            message.setGroup(group);
            ChatMessage saved = chatMessageRepository.save(message);

            // Use ChatService's mapToDto to include the nested reply preview
            ChatMessageDto responseDto = chatService.mapToDto(saved);
            messagingTemplate.convertAndSend("/topic/group/" + group.getId(), responseDto);

            // Broadcast updated unread badge to group members (except sender)
            List<ChatGroupMember> members = groupMemberRepository.findByGroupId(group.getId());
            for (ChatGroupMember member : members) {
                if (!member.getUser().getId().equals(sender.getId())) {
                    chatSidebarService.broadcastLiveUnreadCount(member.getUser());
                }
            }

            log.info("✅ Group message saved and routed to Group ID: {}", group.getId());

        } else if (dto.getRecipientId() != null) {
            User recipient = userRepository.findById(dto.getRecipientId()).orElseThrow(() -> new RuntimeException("Recipient not found"));
            message.setRecipient(recipient);
            ChatMessage saved = chatMessageRepository.save(message);

            // Use ChatService's mapToDto to include the nested reply preview
            ChatMessageDto responseDto = chatService.mapToDto(saved);

            messagingTemplate.convertAndSendToUser(recipient.getEmployeeCode(), "/queue/chat", responseDto);
            messagingTemplate.convertAndSendToUser(sender.getEmployeeCode(), "/queue/chat", responseDto);

            // Broadcast updated unread badge to direct recipient
            chatSidebarService.broadcastLiveUnreadCount(recipient);

            log.info("✅ Direct message saved and routed to User: {}", recipient.getEmployeeCode());
        }
    }
}