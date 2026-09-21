package com.crm.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallHippoReminderResponse {

    private boolean success;

    private String message;

    private Object data;
}