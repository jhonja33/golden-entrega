package com.fincard.authorization.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.fincard.authorization.config.SecurityConfig;
import com.fincard.authorization.dto.TokenResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService implements ITokenService {

	private static final long TOKEN_DURATION_SECONDS = 900;

	private final JwtEncoder jwtEncoder;

	@Override
	public TokenResponse generateToken(Authentication authentication) {

		Instant now = Instant.now();

		String scope = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.equals("payments:authorize")).collect(Collectors.joining(" "));

		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(SecurityConfig.JWT_ISSUER).subject(authentication.getName())
				.audience(List.of(SecurityConfig.JWT_AUDIENCE)).issuedAt(now).notBefore(now)
				.expiresAt(now.plusSeconds(TOKEN_DURATION_SECONDS)).id(UUID.randomUUID().toString())
				.claim("scope", scope).build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new TokenResponse(token, "Bearer", TOKEN_DURATION_SECONDS);
	}
}