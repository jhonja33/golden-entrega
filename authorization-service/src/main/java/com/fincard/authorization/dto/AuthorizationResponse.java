package com.fincard.authorization.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    private String transactionId;
    private String pan;
    private String status;
    private String responseCode;
    private String message;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
}