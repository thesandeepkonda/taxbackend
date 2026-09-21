package com.crm.matrix.controller;

import com.crm.matrix.dto.ChatGroupResponseDto;
import com.crm.matrix.dto.CreateChatGroupRequest;
import com.crm.matrix.entity.ChatGroup;
import com.crm.matrix.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/groups")
@RequiredArgsConstructor
public class ChatGroupController {

    private final ChatService chatService;

    @PostMapping("/create")
    public ResponseEntity<ChatGroupResponseDto> createGroup(
            @RequestBody CreateChatGroupRequest request,
            Authentication authentication) {
        ChatGroupResponseDto group = chatService.createGroup(request, authentication);
        return ResponseEntity.ok(group);
    }
}