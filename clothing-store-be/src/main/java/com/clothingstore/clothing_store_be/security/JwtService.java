package com.clothingstore.clothing_store_be.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import com.clothingstore.clothing_store_be.config.JwtConfig;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();

        JwsHeader header = JwsHeader.with(JwtConfig.JWT_ALGORITHM).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.toUpperCase())
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtProperties.getAccessTokenExpiration()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();

        JwsHeader header = JwsHeader.with(JwtConfig.JWT_ALGORITHM).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
