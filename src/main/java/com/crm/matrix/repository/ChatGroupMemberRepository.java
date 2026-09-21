package com.crm.matrix.repository;

import com.crm.matrix.entity.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {

    // Find all group memberships for a specific user (used for sidebar loading)
    List<ChatGroupMember> findByUserId(Long userId);

    // Find all members belonging to a specific group
    List<ChatGroupMember> findByGroupId(Long groupId);

    // Check if a user is already a member of a specific group
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    Optional<ChatGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    // Find a specific membership record for removal or verification
}