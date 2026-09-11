package com.crm.matrix.controller;

import com.crm.matrix.dto.NotificationResponseDto;
import com.crm.matrix.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        return notificationService.subscribe(authentication);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getUserNotifications(
            Authentication authentication,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(authentication, pageable));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        notificationService.markAsRead(id, authentication);
        return ResponseEntity.noContent().build();
    }
}