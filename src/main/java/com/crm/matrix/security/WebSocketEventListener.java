package com.crm.matrix.security;

import com.crm.matrix.entity.User;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.ChatSidebarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    // Track employeeCode -> Set of active Session IDs (handles multi-tab sessions)
    private static final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatSidebarService chatSidebarService;

    // =========================================================
    // 1. LISTEN TO SessionConnectEvent (Captures Authenticated STOMP Frame)
    // =========================================================
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (principal != null) {
            String employeeCode = principal.getName();

            // Register session ID
            userSessions.computeIfAbsent(employeeCode, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            log.info("===> User Online: {} (Session ID: {})", employeeCode, sessionId);

            // Broadcast ONLINE to everyone if this is their first active session/tab
            if (userSessions.get(employeeCode).size() == 1) {
                broadcastUserStatus(employeeCode, true);
            }
        } else {
            log.info("===> Anonymous WebSocket session attempt. Session ID: {}", sessionId);
        }
    }

    // =========================================================
    // 2. DISCONNECT LISTENER (Handles closing individual tabs)
    // =========================================================
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (principal != null) {
            String employeeCode = principal.getName();

            Set<String> sessions = userSessions.get(employeeCode);
            if (sessions != null) {
                sessions.remove(sessionId);
                log.info("<=== User Disconnected: {} (Session ID: {})", employeeCode, sessionId);

                // If no active sessions remain across any tab, broadcast OFFLINE
                if (sessions.isEmpty()) {
                    userSessions.remove(employeeCode);
                    broadcastUserStatus(employeeCode, false);
                }
            }
        } else {
            log.info("<=== Anonymous WebSocket session closed. Session ID: {}", sessionId);
        }
    }

    // =========================================================
    // 3. SUBSCRIBE LISTENER (Sends instant state to newcomer)
    // =========================================================
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        Principal principal = headerAccessor.getUser();

        if (principal == null || destination == null) {
            return;
        }

        String employeeCode = principal.getName();
        String sessionId = headerAccessor.getSessionId();

        // Topic 1: Online Status
        if (destination.contains("/topic/user-status")) {
            log.info("📡 [SUBSCRIBE] User: {} | Topic: Live Online/Offline Status", employeeCode);
            // Send snapshot of who is currently online to this user
            pushCurrentOnlineUsersSnapshot(employeeCode);
        }
        // Topic 2: Live Unread Count
        else if (destination.contains("/queue/unread-count")) {
            log.info("📩 [SUBSCRIBE] User: {} | Topic: Live Unread Badge Count", employeeCode);
            userRepository.findByEmployeeCode(employeeCode).ifPresent(user -> {
                chatSidebarService.broadcastLiveUnreadCount(user);
                log.info("✅ Pushed initial unread count to user: {}", employeeCode);
            });
        }
    }

    // =========================================================
    // BROADCAST STATUS TO ALL CONNECTED CLIENTS
    // =========================================================
    private void broadcastUserStatus(String employeeCode, boolean isOnline) {
        Optional<User> userOpt = userRepository.findByEmployeeCode(employeeCode);
        if (userOpt.isPresent()) {
            Long userId = userOpt.get().getId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("employeeCode", employeeCode);
            payload.put("isOnline", isOnline);

            log.info("📢 Broadcasting User Status -> User ID: {}, Code: {}, Online: {}", userId, employeeCode, isOnline);
            messagingTemplate.convertAndSend("/topic/user-status", (Object) payload);
        } else {
            log.warn("⚠️ Cannot broadcast status: User not found for employeeCode: {}", employeeCode);
        }
    }

    // =========================================================
    // PUSH SNAPSHOT OF ONLINE USERS TO NEW SUBSCRIBER
    // =========================================================
    private void pushCurrentOnlineUsersSnapshot(String employeeCode) {
        List<Long> onlineUserIds = new ArrayList<>();
        for (String activeCode : userSessions.keySet()) {
            userRepository.findByEmployeeCode(activeCode).ifPresent(u -> onlineUserIds.add(u.getId()));
        }

        Map<String, Object> snapshotPayload = new HashMap<>();
        snapshotPayload.put("onlineUserIds", onlineUserIds);

        messagingTemplate.convertAndSendToUser(
                employeeCode,
                "/queue/user-status-init",
                (Object) snapshotPayload
        );
    }

    public static boolean isUserOnline(String employeeCode) {
        if (employeeCode == null) return false;
        Set<String> sessions = userSessions.get(employeeCode);
        return sessions != null && !sessions.isEmpty();
    }
}