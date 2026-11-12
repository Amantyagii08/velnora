
package com.ruralmarketplace.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.ruralmarketplace.entity.User;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private static final long EXPIRATION_MINUTES = 60;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
            .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("velnora")
                .issuedAt(now)
                .expiresAt(now.plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from( header ,claims)
        ).getTokenValue();
    }

    public long getExpirationSeconds() {
        return EXPIRATION_MINUTES * 60;
    }
}
