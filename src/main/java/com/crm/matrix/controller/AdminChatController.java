package com.crm.matrix.controller;

import com.crm.matrix.dto.ChatContactDto;
import com.crm.matrix.dto.ChatMessageDto;
import com.crm.matrix.dto.SidebarConversationDto;
import com.crm.matrix.service.ChatService;
import com.crm.matrix.service.ChatSidebarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chats")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatSidebarService chatSidebarService;
    private final ChatService chatService;

    // STEP 1: Get list of all employees for the Admin's dropdown/menu
    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ChatContactDto>> getAllEmployees() {
        return ResponseEntity.ok(chatSidebarService.getAllEmployeesForAdmin());
    }

    // STEP 2: Admin clicks an employee -> Return a static view of that employee's sidebar (No WS needed here)
    @GetMapping("/employees/{employeeId}/sidebar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SidebarConversationDto>> getEmployeeSidebar(@PathVariable Long employeeId) {
        return ResponseEntity.ok(chatSidebarService.getAdminViewOfEmployeeConversations(employeeId));
    }

    // STEP 3a: Admin clicks a Direct Message -> Return chat history between those two users
    @GetMapping("/employees/{employeeId}/direct/{partnerId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<ChatMessageDto>> getDirectMessageHistory(
            @PathVariable Long employeeId,
            @PathVariable Long partnerId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getAdminViewOfDirectMessages(employeeId, partnerId, pageable));
    }

    // STEP 3b: Admin clicks a Group Message -> Return the full group chat history
    @GetMapping("/groups/{groupId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<ChatMessageDto>> getGroupMessageHistory(
            @PathVariable Long groupId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getAdminViewOfGroupMessages(groupId, pageable));
    }
}