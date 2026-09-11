package com.crm.matrix.enums;

public enum ClientStatus {

    NEW,
    INTERESTED,
    NOT_INTERESTED,
    FOLLOW_UP,
    NOT_LIFTED,
    CALL_BACK,
    DOCUMENTS_PENDING,
    DOCUMENTS_RECEIVED,

    PREPARATION_ASSIGNED,
    PREPARATION_IN_PROGRESS,
    DRAFT_READY,       // Prep team uploaded a draft
    DRAFT_REJECTED,    // Admin wants changes (Draft 2 needed)
    DRAFT_APPROVED,
    COMPLETED
}