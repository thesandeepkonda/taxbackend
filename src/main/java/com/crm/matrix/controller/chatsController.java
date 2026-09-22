package com.crm.matrix.controller;


import com.crm.matrix.dto.ChatMessageDto;
import com.crm.matrix.dto.GroupMembersResponseDto;
import com.crm.matrix.dto.SendMessageRequest;
import com.crm.matrix.repository.ChatGroupMemberRepository;
import com.crm.matrix.repository.ChatGroupRepository;
import com.crm.matrix.repository.ChatMessageRepository;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.ChatService;
import com.crm.matrix.service.ChatSidebarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class chatsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final UserRepository userRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final ChatService chatService;
    private final ChatSidebarService chatSidebarService;


    @PutMapping("/api/chat/direct/{partnerId}/read")
    public ResponseEntity<Void> markDirectAsRead(@PathVariable Long partnerId, Authentication authentication) {
        chatService.markDirectConversationAsRead(partnerId, authentication);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/chat/group/{groupId}/read")
    public ResponseEntity<Void> markGroupAsRead(@PathVariable Long groupId, Authentication authentication) {
        chatService.markGroupConversationAsRead(groupId, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chat/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> deleteMessageForEveryone(@PathVariable Long messageId, Authentication authentication) {
        ChatMessageDto updatedDto = chatService.deleteMessageForEveryone(messageId, authentication);
        return ResponseEntity.ok(updatedDto);
    }

    @PutMapping("/api/chat/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> editMessage(@PathVariable Long messageId, @RequestBody Map<String, String> payload, Authentication authentication) {

        String newContent = payload.get("content");
        if (newContent == null || newContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }


        ChatMessageDto updatedMessage = chatService.editMessage(messageId, newContent, authentication);
        return ResponseEntity.ok(updatedMessage);
    }

    @PostMapping("/api/chat/messages/{messageId}/forward")
    public ResponseEntity<List<ChatMessageDto>> forwardMessage(@PathVariable Long messageId, @RequestBody Map<String, List<Long>> payload, Authentication authentication) {

        List<Long> recipientIds = payload.get("recipientIds");
        List<Long> groupIds = payload.get("groupIds");

        if ((recipientIds == null || recipientIds.isEmpty()) && (groupIds == null || groupIds.isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        List<ChatMessageDto> forwardedMessages = chatService.forwardMessageToMultiple(messageId, recipientIds, groupIds, authentication);
        return ResponseEntity.ok(forwardedMessages);
    }


    @PutMapping("/api/chat/messages/{messageId}/react")
    public ResponseEntity<ChatMessageDto> reactToMessage(@PathVariable Long messageId, @RequestBody Map<String, String> payload, Authentication authentication) {

        String emoji = payload.get("emoji");
        ChatMessageDto updatedMessage = chatService.reactToMessage(messageId, emoji, authentication);

        return ResponseEntity.ok(updatedMessage);
    }

    @GetMapping("/api/chat/group/{groupId}/members")
    public ResponseEntity<GroupMembersResponseDto> getGroupMembers(@PathVariable Long groupId, Authentication authentication) {

        GroupMembersResponseDto response = chatService.getGroupMembers(groupId, authentication);
        return ResponseEntity.ok(response);

    }
    // =========================================================
    // UPLOAD FILE MESSAGE ENDPOINT
    // =========================================================
    @PostMapping("/api/chat/messages/file")
    public ResponseEntity<ChatMessageDto> uploadFileMessage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "recipientId", required = false) Long recipientId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            @RequestParam(value = "isForwarded", defaultValue = "false") Boolean isForwarded,
            Authentication authentication) {

        if (recipientId == null && groupId == null) {
            return ResponseEntity.badRequest().build();
        }

        // Passes the file and data to your existing ChatService method
        ChatMessageDto fileMessage = chatService.sendFileMessage(
                file, recipientId, groupId, content, replyToId, isForwarded, authentication
        );

        return ResponseEntity.ok(fileMessage);
    }

    @GetMapping("/api/chat/files/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> getChatFile(
            @PathVariable String fileName,
            @RequestParam(required = false, defaultValue = "false") boolean download) {

        org.springframework.core.io.Resource resource = chatService.getChatFileResource(fileName);

        try {
            Path filePath = Paths.get("uploads/chat-media").resolve(fileName);
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                contentType = "application/octet-stream"; // Fallback for unknown types
            }

            String disposition = download ? "attachment" : "inline";

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (java.io.IOException e) {
            throw new RuntimeException("Error determining file content type", e);
        }
    }

    @GetMapping("/api/chat/messages/search")
    public ResponseEntity<List<ChatMessageDto>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long groupId,
            Authentication authentication) {

        List<ChatMessageDto> results = chatService.searchMessages(keyword, partnerId, groupId, authentication);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/api/chat/direct/{partnerId}/clear")
    public ResponseEntity<Void> clearDirectChat(
            @PathVariable Long partnerId,
            Authentication authentication) {

        chatService.clearDirectChat(partnerId, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chat/group/{groupId}/clear")
    public ResponseEntity<Void> clearGroupChat(
            @PathVariable Long groupId,
            Authentication authentication) {

        chatService.clearGroupChat(groupId, authentication);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/api/chat/messages/{messageId}/react")
    public ResponseEntity<ChatMessageDto> removeReaction(
            @PathVariable Long messageId,
            Authentication authentication) {

        // Passing a blank/empty emoji string triggers your existing toggle-off logic in ChatService
        ChatMessageDto updatedMessage = chatService.reactToMessage(messageId, "", authentication);
        return ResponseEntity.ok(updatedMessage);
    }

    @PutMapping("/api/chat/messages/{messageId}/pin")
    public ResponseEntity<ChatMessageDto> togglePinMessage(
            @PathVariable Long messageId,
            Authentication authentication) {

        ChatMessageDto updatedMessage = chatService.togglePinMessage(messageId, authentication);
        return ResponseEntity.ok(updatedMessage);
    }
    @GetMapping("/api/chat/pinned")
    public ResponseEntity<List<ChatMessageDto>> getPinnedMessages(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long groupId,
            Authentication authentication) {

        List<ChatMessageDto> pinned = chatService.getPinnedMessages(partnerId, groupId, authentication);
        return ResponseEntity.ok(pinned);
    }
    @PostMapping("/api/chat/messages")
    public ResponseEntity<ChatMessageDto> sendTextMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {

        if (request.getRecipientId() == null && request.getGroupId() == null) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessageDto sentMessage = chatService.sendTextMessage(
                request.getRecipientId(),
                request.getGroupId(),
                request.getContent(),
                request.getReplyToId(),
                request.getIsForwarded(),
                authentication
        );

        return ResponseEntity.ok(sentMessage);
    }


}
