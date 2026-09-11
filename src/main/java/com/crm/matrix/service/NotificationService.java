package com.crm.matrix.service;

import com.crm.matrix.dto.NotificationResponseDto;
import com.crm.matrix.entity.Notification;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.NotificationRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Store active SSE connections mapped by userId
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    private User getLoggedInUser(Authentication authentication) {
        return userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter subscribe(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required for SSE subscription");
        }

        // 1. Extract the user directly from the JWT Security Context Principal
        // This avoids touching the database entirely!
        com.crm.matrix.security.CustomUserDetails userDetails =
                (com.crm.matrix.security.CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected to real-time notifications"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    // Helper to ensure database connection is released immediately after lookup
    @Transactional(readOnly = true)
    public User fetchUserForSubscription(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required for SSE subscription");
        }
        return userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }


    @Transactional
    public void sendNotification(User recipient, String title, String message, String type,String link) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        notification.setIsRead(false);
        
        Notification saved = notificationRepository.save(notification);

        // Push real-time event if user is connected
        SseEmitter emitter = emitters.get(recipient.getId());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(mapToDto(saved)));
            } catch (IOException e) {
                emitters.remove(recipient.getId());
            }
        }
    }


    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getUserNotifications(Authentication authentication, Pageable pageable) {
        User user = getLoggedInUser(authentication);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToDto);
    }

    // =========================================================
    // 4. MARK AS READ
    // =========================================================
    @Transactional
    public void markAsRead(Long notificationId, Authentication authentication) {
        User user = getLoggedInUser(authentication);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to modify this notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private NotificationResponseDto mapToDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .link(notification.getLink())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}