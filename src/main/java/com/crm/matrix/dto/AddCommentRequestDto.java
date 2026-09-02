package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCommentRequestDto {

    @NotBlank
    @Size(max = 2000)
    private String comment;
}