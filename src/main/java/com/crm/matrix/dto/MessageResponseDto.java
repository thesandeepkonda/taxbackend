package com.crm.matrix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponseDto {

    private boolean success;

    private String message;

}