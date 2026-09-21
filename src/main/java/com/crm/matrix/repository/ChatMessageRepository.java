package com.crm.matrix.repository;

import com.crm.matrix.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 2. Paginated Group Conversation (Sorted newest first)
    Page<ChatMessage> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    // 3. For the Sidebar: Fetch all DMs for the user to extract the latest partners
    @Query("SELECT m FROM ChatMessage m WHERE m.sender.id = :userId OR m.recipient.id = :userId ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllDirectMessagesForUser(@Param("userId") Long userId);

    // 4. For the Sidebar: Fetch the absolute latest message for a specific group
    Optional<ChatMessage> findTopByGroupIdOrderByCreatedAtDesc(Long groupId);

    int countBySenderIdAndRecipientIdAndIsReadFalse(Long senderId, Long recipientId);

    // ---> GROUP MESSAGE UNREAD COUNT (High Water Mark) <---
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.group.id = :groupId AND (:lastReadAt IS NULL OR m.createdAt > :lastReadAt)")
    int countUnreadGroupMessages(@Param("groupId") Long groupId, @Param("lastReadAt") LocalDateTime lastReadAt);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.sender.id = :partnerId AND m.recipient.id = :userId AND m.isRead = false")
    void markDirectMessagesAsRead(@Param("partnerId") Long partnerId, @Param("userId") Long userId);

    // This fixes the N+1 performance issue
    int countByRecipientIdAndIsReadFalse(Long recipientId);

    // =========================================================================
    // PAGINATED CONVERSATION WITH EXPLICIT COUNT QUERY (Fixes Hibernate 6 NPE)
    // =========================================================================
    @Query(value = "SELECT m FROM ChatMessage m WHERE " + "((m.sender.id = :userId AND m.recipient.id = :partnerId) OR " + "(m.sender.id = :partnerId AND m.recipient.id = :userId)) " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :userId) " + "ORDER BY m.createdAt DESC", countQuery = "SELECT count(m) FROM ChatMessage m WHERE " + "((m.sender.id = :userId AND m.recipient.id = :partnerId) OR " + "(m.sender.id = :partnerId AND m.recipient.id = :userId)) " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :userId)")
    Page<ChatMessage> findConversation(@Param("userId") Long userId, @Param("partnerId") Long partnerId, Pageable pageable);

    // 2. Fetch messages to clear
    @Query("SELECT m FROM ChatMessage m WHERE " + "((m.sender.id = :user1 AND m.recipient.id = :user2) OR " + "(m.sender.id = :user2 AND m.recipient.id = :user1)) " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :user1)")
    List<ChatMessage> findUnclearedDirectMessages(@Param("user1") Long user1, @Param("user2") Long user2);

    // 3. Search Direct Messages
    @Query("SELECT m FROM ChatMessage m WHERE " + "((m.sender.id = :userId AND m.recipient.id = :partnerId) OR " + "(m.sender.id = :partnerId AND m.recipient.id = :userId)) " + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :userId) " + "ORDER BY m.createdAt DESC")
    List<ChatMessage> searchDirectMessages(@Param("userId") Long userId, @Param("partnerId") Long partnerId, @Param("keyword") String keyword);

    // 4. Search Group Messages
    @Query("SELECT m FROM ChatMessage m WHERE m.group.id = :groupId " + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :userId) " + "ORDER BY m.createdAt DESC")
    List<ChatMessage> searchGroupMessages(@Param("userId") Long userId, @Param("groupId") Long groupId, @Param("keyword") String keyword);


    @Query("SELECT m FROM ChatMessage m WHERE m.group.id = :groupId AND m.isPinned = true ORDER BY m.createdAt DESC")
    List<ChatMessage> findPinnedGroupMessages(@Param("groupId") Long groupId);

    // Fetch pinned messages in a Direct Chat
    @Query("SELECT m FROM ChatMessage m WHERE " + "((m.sender.id = :userId AND m.recipient.id = :partnerId) OR " + "(m.sender.id = :partnerId AND m.recipient.id = :userId)) " + "AND m.isPinned = true " + "AND NOT EXISTS (SELECT 1 FROM ChatMessage m2 JOIN m2.clearedBy cb WHERE m2.id = m.id AND cb = :userId) " + "ORDER BY m.createdAt DESC")
    List<ChatMessage> findPinnedDirectMessages(@Param("userId") Long userId, @Param("partnerId") Long partnerId);
}