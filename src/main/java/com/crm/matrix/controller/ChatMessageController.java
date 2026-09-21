package com.crm.matrix.controller;

import com.crm.matrix.dto.ChatMessageDto;
import com.crm.matrix.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @GetMapping("/direct/{partnerId}")
    public ResponseEntity<Page<ChatMessageDto>> getDirectMessages(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, // 50 messages per page is standard for chat
            Authentication authentication) {

        // Sorting by 'createdAt' descending ensures the newest messages load at the bottom
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ChatMessageDto> messages = chatService.getDirectMessages(partnerId, pageable, authentication);

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Page<ChatMessageDto>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ChatMessageDto> messages = chatService.getGroupMessages(groupId, pageable, authentication);

        return ResponseEntity.ok(messages);
    }
}