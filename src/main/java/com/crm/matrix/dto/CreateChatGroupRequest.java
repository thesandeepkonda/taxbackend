package com.crm.matrix.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateChatGroupRequest {
    private String name;
    private String description;
    private List<Long> memberIds; // IDs of users to add to the group
}