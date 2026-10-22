package com.fincard.authorization.service;

import org.springframework.security.core.Authentication;

import com.fincard.authorization.dto.TokenResponse;

public interface ITokenService {

    public TokenResponse generateToken(Authentication authentication);
}