package com.crm.matrix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReviewRequestDto {

    @NotBlank(message = "Rejection comment is required")
    private String comment;
}