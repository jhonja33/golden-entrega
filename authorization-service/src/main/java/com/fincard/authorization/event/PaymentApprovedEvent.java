package com.fincard.authorization.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentApprovedEvent(String pan, String transactionId, 
		BigDecimal amount, String currency,Instant createdAt) {}