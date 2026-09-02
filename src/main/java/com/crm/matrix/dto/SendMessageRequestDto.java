package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequestDto {

    @NotBlank
    private String message;
}