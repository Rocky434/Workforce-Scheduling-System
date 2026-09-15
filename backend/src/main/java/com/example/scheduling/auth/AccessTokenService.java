package com.example.scheduling.auth;

import com.example.scheduling.user.AppUser;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {
    private final JwtEncoder jwt;
    private final Duration ttl;

    public AccessTokenService(JwtEncoder jwt, @Value("${app.access-token-ttl:15m}") Duration ttl) {
        this.jwt = jwt;
        this.ttl = ttl;
    }

    public String issue(AppUser user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("scheduling")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("role", user.getRole().name())
                .claim("token_type", "access")
                .build();
        return jwt.encode(JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(), claims)).getTokenValue();
    }
}
