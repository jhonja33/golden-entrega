package com.fincard.authorization.dto;

import java.math.BigDecimal;

public record TransactionApprovedMessage(
        int schemaVersion,
        String eventId,
        String eventType,
        String transactionId,
        BigDecimal amount,
        String currency,
        String status,
        String responseCode,
        String createdAt
) {
}