package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCommentRequestDto {

    @NotBlank(message = "Comment cannot be empty")
    private String comment;
}