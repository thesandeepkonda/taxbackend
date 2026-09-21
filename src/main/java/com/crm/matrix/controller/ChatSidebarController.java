package com.crm.matrix.controller;

import com.crm.matrix.dto.ChatContactDto;
import com.crm.matrix.dto.SidebarConversationDto;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.ChatSidebarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatSidebarController {

    private final ChatSidebarService chatSidebarService;
    private final UserRepository  userRepository;

    @GetMapping("/sidebar")
    public ResponseEntity<List<SidebarConversationDto>> getSidebar(Authentication authentication) {
        return ResponseEntity.ok(chatSidebarService.getSidebarConversations(authentication));
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactDto>> getContacts(Authentication authentication) {
        return ResponseEntity.ok(chatSidebarService.getAvailableContacts(authentication));
    }
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getInitialUnreadCount(Authentication authentication) {
        User user = userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        int totalUnread = chatSidebarService.getTotalUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", totalUnread));
    }
}