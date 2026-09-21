package com.crm.matrix.repository;

import com.crm.matrix.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    // Basic search functionality for groups
    List<ChatGroup> findByNameContainingIgnoreCase(String name);
    
}