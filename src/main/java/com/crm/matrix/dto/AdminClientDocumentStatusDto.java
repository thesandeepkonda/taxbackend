package com.crm.matrix.dto;

import com.crm.matrix.enums.ClientStatus;
import com.crm.matrix.enums.DocumentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminClientDocumentStatusDto {

    private Long clientId;

    private String name;

    private String email;

    private String phone;

    private long totalDocuments;

    private long submittedDocuments;

    private long pendingDocuments;

    private String documentStatus;
    private  String currentStage;
    private ClientStatus status;
}