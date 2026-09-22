package com.crm.matrix.service;

import com.crm.matrix.dto.*;
import com.crm.matrix.entity.ChatGroup;
import com.crm.matrix.entity.ChatGroupMember;
import com.crm.matrix.entity.ChatMessage;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.MessageType;
import com.crm.matrix.repository.ChatGroupMemberRepository;
import com.crm.matrix.repository.ChatGroupRepository;
import com.crm.matrix.repository.ChatMessageRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Inject the Sidebar service to trigger live updates
    private final ChatSidebarService chatSidebarService;

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getDirectMessages(Long partnerId, Pageable pageable, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        log.debug("Fetching paginated DMs between user {} and partner {}", currentUser.getId(), partnerId);

        Page<ChatMessage> messages = chatMessageRepository.findConversation(currentUser.getId(), partnerId, pageable);
        return messages.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getGroupMessages(Long groupId, Pageable pageable, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        log.debug("Fetching paginated group messages for group {} by user {}", groupId, currentUser.getId());

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            log.warn("Unauthorized chat access attempt: User {} -> Group {}", currentUser.getId(), groupId);
            throw new SecurityException("You are not a member of this group");
        }

        // Descending order is crucial for infinite-scroll UIs
        Page<ChatMessage> messages = chatMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        return messages.map(this::mapToDto);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmployeeCode(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    public ChatMessageDto mapToDto(ChatMessage msg) {
        User sender = msg.getSender();
        String senderName = sender.getFirstName() + (sender.getLastName() != null ? " " + sender.getLastName() : "");

        ChatMessageDto replyPreview = null;
        if (msg.getReplyTo() != null) {
            ChatMessage r = msg.getReplyTo();
            User rSender = r.getSender();
            String rSenderName = rSender.getFirstName() + (rSender.getLastName() != null ? " " + rSender.getLastName() : "");
            replyPreview = ChatMessageDto.builder()
                    .id(r.getId())
                    .senderId(rSender.getId())
                    .senderName(rSenderName.trim())
                    .content(Boolean.TRUE.equals(r.getIsDeleted()) ? "This message was deleted" : r.getContent())
                    .type(r.getType())
                    .build();
        }

        return ChatMessageDto.builder()
                .id(msg.getId())
                .senderId(sender.getId())
                .senderName(senderName.trim())
                .recipientId(msg.getRecipient() != null ? msg.getRecipient().getId() : null)
                .groupId(msg.getGroup() != null ? msg.getGroup().getId() : null)
                .content(msg.getContent())
                .fileUrl(msg.getFileUrl())
                .fileName(msg.getFileName())
                .type(msg.getType())
                .timestamp(msg.getCreatedAt())
                .isRead(msg.getIsRead())
                .replyToId(msg.getReplyTo() != null ? msg.getReplyTo().getId() : null)
                .replyToMessage(replyPreview)
                .isForwarded(msg.getIsForwarded() != null ? msg.getIsForwarded() : false)
                .isDeleted(msg.getIsDeleted() != null ? msg.getIsDeleted() : false)
                .isEdited(msg.getIsEdited() != null ? msg.getIsEdited() : false)
                .editedAt(msg.getEditedAt())
                .reactions(msg.getReactions())
                .isPinned(msg.getIsPinned()) // ---> MAPPED PIN STATUS <---
                .build();
    }

    @Transactional
    public ChatGroupResponseDto createGroup(CreateChatGroupRequest request, Authentication authentication) {
        User currentUser = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }

        // 1. Create and save the group
        ChatGroup group = new ChatGroup();
        group.setName(request.getName().trim());
        group.setDescription(request.getDescription());
        group.setCreatedAt(LocalDateTime.now());
        group.setCreatedBy(currentUser); // Securely set creator

        ChatGroup savedGroup = chatGroupRepository.save(group);

        // 2. Ensure creator and members are added
        addMemberToGroup(savedGroup, currentUser);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                if (memberId.equals(currentUser.getId())) continue;
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + memberId));
                addMemberToGroup(savedGroup, member);
            }
        }

        // 3. Map to DTO safely while the database transaction is still active
        String creatorName = currentUser.getFirstName() +
                (currentUser.getLastName() != null ? " " + currentUser.getLastName() : "");

        return ChatGroupResponseDto.builder()
                .id(savedGroup.getId())
                .name(savedGroup.getName())
                .description(savedGroup.getDescription())
                .createdAt(savedGroup.getCreatedAt())
                .createdById(currentUser.getId())
                .createdByName(creatorName.trim())
                .build();
    }

    private void addMemberToGroup(ChatGroup group, User user) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            ChatGroupMember membership = new ChatGroupMember();
            membership.setGroup(group);
            membership.setUser(user);
            membership.setCreatedAt(LocalDateTime.now());
            groupMemberRepository.save(membership);
        }
    }

    @Transactional
    public ChatMessageDto sendFileMessage(MultipartFile file, Long recipientId, Long groupId, String content, Long replyToId, Boolean isForwarded, Authentication authentication) {
        User sender = getAuthenticatedUser(authentication);

        // 1. FILE UPLOAD LOGIC
        Path chatUploadDirectory = Paths.get("uploads/chat-media");
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String storedName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(chatUploadDirectory);
            Path filePath = chatUploadDirectory.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload chat file", e);
        }
        String fileUrl = "/api/chat/files/" + storedName;

        // 2. DYNAMICALLY DETECT TYPE
        com.crm.matrix.enums.MessageType type = com.crm.matrix.enums.MessageType.DOCUMENT;
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.startsWith("image/")) type = com.crm.matrix.enums.MessageType.IMAGE;
            else if (contentType.startsWith("audio/")) type = com.crm.matrix.enums.MessageType.AUDIO;
            else if (contentType.startsWith("video/")) type = com.crm.matrix.enums.MessageType.VIDEO;
        }

        // 3. CREATE & SAVE CHAT MESSAGE
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setContent(content != null ? content : originalName);
        message.setFileUrl(fileUrl);
        message.setFileName(originalName);
        message.setType(type);
        message.setIsRead(false);
        message.setIsDeleted(false);
        message.setIsEdited(false);

        // Handle Reply/Forward for files
        message.setIsForwarded(Boolean.TRUE.equals(isForwarded));
        if (replyToId != null) {
            chatMessageRepository.findById(replyToId).ifPresent(message::setReplyTo);
        }

        if (recipientId != null) {
            User recipient = userRepository.findById(recipientId).orElseThrow(() -> new RuntimeException("Recipient not found"));
            message.setRecipient(recipient);
        } else if (groupId != null) {
            ChatGroup group = chatGroupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
            message.setGroup(group);
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        // 4. BROADCAST TO WEBSOCKET LIVE & UPDATE UNREAD BADGES
        if (groupId != null) {
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);

            // Push badge update to all group members EXCEPT the sender
            List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);
            for (ChatGroupMember member : members) {
                if (!member.getUser().getId().equals(sender.getId())) {
                    chatSidebarService.broadcastLiveUnreadCount(member.getUser());
                }
            }
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            // Sender needs it for UI update (especially for file previews)
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);

            // Push badge update to the direct recipient
            chatSidebarService.broadcastLiveUnreadCount(message.getRecipient());
        }

        return dto;
    }

    // =========================================================
    // MARK DIRECT CONVERSATION AS READ & BROADCAST "SEEN"
    // =========================================================
    @Transactional
    public void markDirectConversationAsRead(Long partnerId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        // 1. Get exact IDs of messages that are currently unread (Sent by partner, received by current user)
        List<Long> newlyReadMessageIds = chatMessageRepository.findUnreadMessageIds(partnerId, currentUser.getId());

        if (newlyReadMessageIds.isEmpty()) {
            return; // Nothing to update or broadcast
        }

        // 2. Update database
        chatMessageRepository.markDirectMessagesAsRead(partnerId, currentUser.getId());

        // 3. Clear the unread badge for the person who just opened the chat
        chatSidebarService.broadcastLiveUnreadCount(currentUser);

        // 4. ---> BROADCAST LIVE "SEEN" RECEIPT TO THE PARTNER WITH EXACT MESSAGE IDs <---
        User partner = userRepository.findById(partnerId).orElse(null);
        if (partner != null) {
            java.util.Map<String, Object> receipt = new java.util.HashMap<>();
            receipt.put("type", "DIRECT_READ");
            receipt.put("readerId", currentUser.getId());
            receipt.put("messageIds", newlyReadMessageIds); // Frontend uses this to show blue ticks!
            receipt.put("seenAt", LocalDateTime.now());

            // Ping the partner's WebSocket so their sent messages instantly turn into "Seen"
            messagingTemplate.convertAndSendToUser(
                    partner.getEmployeeCode(),
                    "/queue/chat-receipts",
                    receipt
            );
        }
    }

    // =========================================================
    // MARK GROUP CONVERSATION AS READ & BROADCAST "SEEN"
    // =========================================================
    @Transactional
    public void markGroupConversationAsRead(Long groupId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        ChatGroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        LocalDateTime previousReadAt = membership.getLastReadAt();
        LocalDateTime now = LocalDateTime.now();

        // 1. Find exactly which message IDs the user is reading right now
        List<Long> newlyReadMessageIds;
        if (previousReadAt == null) {
            newlyReadMessageIds = chatMessageRepository.findAllGroupMessageIds(groupId);
        } else {
            newlyReadMessageIds = chatMessageRepository.findUnreadGroupMessageIds(groupId, previousReadAt);
        }

        // 2. Update DB
        membership.setLastReadAt(now);
        groupMemberRepository.save(membership);

        // 3. Clear the unread badge on the frontend
        chatSidebarService.broadcastLiveUnreadCount(currentUser);

        if (newlyReadMessageIds.isEmpty()) {
            return; // No new messages were read, prevent spamming websockets
        }

        // 4. ---> BROADCAST LIVE "SEEN" RECEIPT TO THE GROUP WITH EXACT MESSAGE IDs <---
        java.util.Map<String, Object> receipt = new java.util.HashMap<>();
        receipt.put("type", "GROUP_READ");
        receipt.put("groupId", groupId);
        receipt.put("readerId", currentUser.getId());
        receipt.put("readerName", (currentUser.getFirstName() + " " + (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim());
        receipt.put("messageIds", newlyReadMessageIds); // Frontend uses this to add avatars under the message!
        receipt.put("seenAt", now);

        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/receipts", (Object) receipt);
    }

    @Transactional
    public ChatMessageDto editMessage(Long messageId, String newContent, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only edit your own messages");
        }

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot edit a deleted message");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        // Broadcast the updated message
        if (message.getGroup() != null) {
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);
        }

        return dto;
    }

    // =========================================================
    // DELETE FOR EVERYONE
    // =========================================================
    @Transactional
    public ChatMessageDto deleteMessageForEveryone(Long messageId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Security check: Only the sender can delete their own message
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Soft Delete: Overwrite content but keep the record so UI updates
        message.setIsDeleted(true);
        message.setContent("This message was deleted");
        message.setFileUrl(null);
        message.setFileName(null);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        // Broadcast the deleted message object to WebSockets so the UI hides the original
        if (message.getGroup() != null) {
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);
        }

        return dto;
    }

    // =========================================================
    // FORWARD MESSAGE (Handles Multiple Users and Groups)
    // =========================================================
    @Transactional
    public List<ChatMessageDto> forwardMessageToMultiple(Long originalMessageId, List<Long> recipientIds, List<Long> groupIds, Authentication authentication) {
        User sender = getAuthenticatedUser(authentication);

        ChatMessage originalMessage = chatMessageRepository.findById(originalMessageId)
                .orElseThrow(() -> new RuntimeException("Original message not found"));

        if (Boolean.TRUE.equals(originalMessage.getIsDeleted())) {
            throw new RuntimeException("Cannot forward a deleted message");
        }

        List<ChatMessageDto> forwardedMessages = new java.util.ArrayList<>();

        // 1. Forward to all selected Individual Users
        if (recipientIds != null && !recipientIds.isEmpty()) {
            for (Long recipientId : recipientIds) {
                User recipient = userRepository.findById(recipientId).orElse(null);
                if (recipient != null) {
                    ChatMessage forwardedMsg = createForwardClone(originalMessage, sender);
                    forwardedMsg.setRecipient(recipient);

                    ChatMessage saved = chatMessageRepository.save(forwardedMsg);
                    ChatMessageDto dto = mapToDto(saved);
                    forwardedMessages.add(dto);

                    // Broadcast
                    messagingTemplate.convertAndSendToUser(recipient.getEmployeeCode(), "/queue/chat", dto);
                    messagingTemplate.convertAndSendToUser(sender.getEmployeeCode(), "/queue/chat", dto);
                    chatSidebarService.broadcastLiveUnreadCount(recipient);
                }
            }
        }

        // 2. Forward to all selected Groups
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Long groupId : groupIds) {
                ChatGroup group = chatGroupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    ChatMessage forwardedMsg = createForwardClone(originalMessage, sender);
                    forwardedMsg.setGroup(group);

                    ChatMessage saved = chatMessageRepository.save(forwardedMsg);
                    ChatMessageDto dto = mapToDto(saved);
                    forwardedMessages.add(dto);

                    // Broadcast
                    messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);

                    List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);
                    for (ChatGroupMember member : members) {
                        if (!member.getUser().getId().equals(sender.getId())) {
                            chatSidebarService.broadcastLiveUnreadCount(member.getUser());
                        }
                    }
                }
            }
        }

        return forwardedMessages;
    }

    // Helper method to clone the message securely
    private ChatMessage createForwardClone(ChatMessage original, User sender) {
        ChatMessage clone = new ChatMessage();
        clone.setSender(sender);
        clone.setContent(original.getContent());
        clone.setFileUrl(original.getFileUrl());
        clone.setFileName(original.getFileName());
        clone.setType(original.getType());
        clone.setIsRead(false);
        clone.setIsDeleted(false);
        clone.setIsEdited(false);
        clone.setIsForwarded(true); // Mark as forwarded
        return clone;
    }

    // =========================================================
    // REACT TO MESSAGE (Add/Update/Remove Emojis)
    // =========================================================
    @Transactional
    public ChatMessageDto reactToMessage(Long messageId, String emoji, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot react to a deleted message");
        }

        // If emoji is null or blank, it acts as a "remove reaction" toggle
        if (emoji == null || emoji.trim().isEmpty()) {
            message.getReactions().remove(currentUser.getId());
        } else {
            // Put adds a new reaction or overwrites the user's existing reaction
            message.getReactions().put(currentUser.getId(), emoji.trim());
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        // Broadcast the updated message so UI reflects the reaction instantly
        if (message.getGroup() != null) {
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);
        }

        return dto;
    }
    // =========================================================
    // GET GROUP MEMBERS & COUNT
    // =========================================================
    @Transactional(readOnly = true)
    public GroupMembersResponseDto getGroupMembers(Long groupId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new SecurityException("You are not a member of this group");
        }

        List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);

        List<GroupMemberDto> memberDtos = members.stream().map(member -> {
            User user = member.getUser();
            String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");

            return GroupMemberDto.builder()
                    .userId(user.getId())
                    .name(fullName.trim())
                    .employeeCode(user.getEmployeeCode())
                    .build();
        }).toList();

        // Wrap the list and the size in the new DTO
        return GroupMembersResponseDto.builder()
                .count(memberDtos.size())
                .members(memberDtos)
                .build();
    }


    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource getChatFileResource(String fileName) {
        try {
            // Decode the filename in case it contains %20 or +
            String decodedFileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8.name());

            // Resolve the path exactly where sendFileMessage saves it
            Path filePath = Paths.get("uploads/chat-media").resolve(decodedFileName).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or is unreadable: " + decodedFileName);
            }
            return resource;
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("Error reading file: " + fileName, e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding file name: " + fileName, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> searchMessages(String keyword, Long partnerId, Long groupId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<ChatMessage> results;
        if (groupId != null) {
            results = chatMessageRepository.searchGroupMessages(currentUser.getId(), groupId, keyword.trim());
        } else if (partnerId != null) {
            results = chatMessageRepository.searchDirectMessages(currentUser.getId(), partnerId, keyword.trim());
        } else {
            throw new IllegalArgumentException("Must provide either partnerId or groupId to search");
        }

        return results.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void clearDirectChat(Long partnerId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        // Fetch all messages between these two users that aren't already cleared
        List<ChatMessage> messages = chatMessageRepository.findUnclearedDirectMessages(currentUser.getId(), partnerId);

        // Mark them as "cleared" for the current user only
        for (ChatMessage msg : messages) {
            msg.getClearedBy().add(currentUser.getId());
        }

        chatMessageRepository.saveAll(messages);
    }
    @Transactional
    public void clearGroupChat(Long groupId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        // Verify user is a member of the group
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new SecurityException("You are not a member of this group");
        }

        // Fetch all messages in the group that haven't been cleared by this user yet
        List<ChatMessage> messages = chatMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, Pageable.unpaged()).getContent();

        // Mark them as "cleared" for the current user only
        for (ChatMessage msg : messages) {
            msg.getClearedBy().add(currentUser.getId());
        }

        chatMessageRepository.saveAll(messages);
    }

    @Transactional
    public ChatMessageDto togglePinMessage(Long messageId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot pin a deleted message");
        }

        boolean currentStatus = Boolean.TRUE.equals(message.getIsPinned());
        message.setIsPinned(!currentStatus);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        if (message.getGroup() != null) {
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);
        }

        return dto;
    }
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getPinnedMessages(Long partnerId, Long groupId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        List<ChatMessage> pinnedMessages;

        if (groupId != null) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
                throw new SecurityException("You are not a member of this group");
            }
            pinnedMessages = chatMessageRepository.findPinnedGroupMessages(groupId);
        } else if (partnerId != null) {
            pinnedMessages = chatMessageRepository.findPinnedDirectMessages(currentUser.getId(), partnerId);
        } else {
            throw new IllegalArgumentException("Must provide either partnerId or groupId");
        }

        return pinnedMessages.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public ChatMessageDto sendTextMessage(Long recipientId, Long groupId, String content, Long replyToId, Boolean isForwarded, Authentication authentication) {
        User sender = getAuthenticatedUser(authentication);

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setContent(content.trim());
        message.setType(MessageType.TEXT);
        message.setIsRead(false);
        message.setIsDeleted(false);
        message.setIsEdited(false);
        message.setIsForwarded(Boolean.TRUE.equals(isForwarded));

        // Handle Reply reference
        if (replyToId != null) {
            chatMessageRepository.findById(replyToId).ifPresent(message::setReplyTo);
        }

        // Handle Recipient or Group
        if (recipientId != null) {
            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));
            message.setRecipient(recipient);
        } else if (groupId != null) {
            ChatGroup group = chatGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            message.setGroup(group);
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = mapToDto(savedMessage);

        // Broadcast to WebSockets
        if (groupId != null) {
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);

            List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);
            for (ChatGroupMember member : members) {
                if (!member.getUser().getId().equals(sender.getId())) {
                    chatSidebarService.broadcastLiveUnreadCount(member.getUser());
                }
            }
        } else {
            messagingTemplate.convertAndSendToUser(message.getRecipient().getEmployeeCode(), "/queue/chat", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getEmployeeCode(), "/queue/chat", dto);
            chatSidebarService.broadcastLiveUnreadCount(message.getRecipient());
        }

        return dto;
    }
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getAdminViewOfDirectMessages(Long employeeId, Long partnerId, Pageable pageable) {
        log.debug("Admin fetching DMs between {} and {}", employeeId, partnerId);
        Page<ChatMessage> messages = chatMessageRepository.findConversation(employeeId, partnerId, pageable);
        return messages.map(this::mapToDto);
    }

    // =========================================================
    // ADMIN VIEW: GROUP MESSAGES (Bypasses group membership check)
    // =========================================================
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getAdminViewOfGroupMessages(Long groupId, Pageable pageable) {
        log.debug("Admin fetching group messages for group {}", groupId);
        Page<ChatMessage> messages = chatMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        return messages.map(this::mapToDto);
    }
}