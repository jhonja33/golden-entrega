package com.fincard.authorization.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal creditLimit;
}