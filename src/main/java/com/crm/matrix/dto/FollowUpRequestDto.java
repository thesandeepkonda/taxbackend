package com.crm.matrix.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowUpRequestDto {

    @NotNull
    private LocalDateTime followUpAt;

    private String remarks;
}