package com.crm.matrix.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDocumentSummaryDto {

    private long totalClients;

    private long submittedClients;

    private long pendingClients;

    private long totalDocuments;

    private long submittedDocuments;
}