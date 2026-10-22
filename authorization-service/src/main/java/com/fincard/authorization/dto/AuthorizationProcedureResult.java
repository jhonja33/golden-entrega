package com.fincard.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorizationProcedureResult {

    private final Long cardId;
    private final String responseCode;
}