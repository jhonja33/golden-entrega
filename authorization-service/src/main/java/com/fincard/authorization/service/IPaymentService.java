package com.fincard.authorization.service;

import com.fincard.authorization.dto.AuthorizationRequest;
import com.fincard.authorization.dto.AuthorizationResponse;

public interface IPaymentService {

    public AuthorizationResponse authorize(
        AuthorizationRequest request
    );
}