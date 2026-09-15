package com.example.scheduling.auth;

import com.example.scheduling.user.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RefreshTokenRepository tokens;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository tokens,
            @Value("${app.refresh-token-ttl:7d}") Duration ttl) {
        this.tokens = tokens;
        this.ttl = ttl;
    }

    @Transactional
    public IssuedRefreshToken issue(AppUser user) {
        return issue(user, Instant.now());
    }

    @Transactional
    public RefreshSession rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw unauthorized();
        }

        var now = Instant.now();
        var current = tokens.findByTokenHashForUpdate(hash(rawToken)).orElseThrow(this::unauthorized);
        if (!current.isUsableAt(now) || !current.getUser().isActive()) {
            throw unauthorized();
        }

        current.revoke(now);
        var replacement = issue(current.getUser(), now);
        return new RefreshSession(current.getUser(), replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        tokens.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    private IssuedRefreshToken issue(AppUser user, Instant now) {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        var rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiresAt = now.plus(ttl);
        tokens.save(new RefreshToken(user, hash(rawToken), expiresAt, now));
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    private String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入狀態已失效，請重新登入");
    }

    public record IssuedRefreshToken(String value, Instant expiresAt) {
    }

    public record RefreshSession(AppUser user, IssuedRefreshToken refreshToken) {
    }
}
