package com.example.scheduling.auth;

import com.example.scheduling.user.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    static final String REFRESH_COOKIE = "refresh_token";
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccessTokenService accessTokens;
    private final RefreshTokenService refreshTokens;
    private final boolean secureCookie;

    public AuthController(UserRepository users, PasswordEncoder passwords, AccessTokenService accessTokens,
            RefreshTokenService refreshTokens,
            @Value("${app.refresh-cookie-secure:false}") boolean secureCookie) {
        this.users = users;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response,
            @RequestHeader(name = "X-Requested-With", required = false) String requestMarker) {
        validateRequestMarker(requestMarker);
        var user = users.findByEmailIgnoreCase(request.email()).filter(AppUser::isActive)
                .filter(u -> passwords.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤"));
        var refreshToken = refreshTokens.issue(user);
        setRefreshCookie(response, refreshToken);
        return authResponse(user);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
            HttpServletResponse response,
            @RequestHeader(name = "X-Requested-With", required = false) String requestMarker) {
        validateRequestMarker(requestMarker);
        var session = refreshTokens.rotate(rawRefreshToken);
        setRefreshCookie(response, session.refreshToken());
        return authResponse(session.user());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
            HttpServletResponse response,
            @RequestHeader(name = "X-Requested-With", required = false) String requestMarker) {
        validateRequestMarker(requestMarker);
        refreshTokens.revoke(rawRefreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
    }

    private AuthResponse authResponse(AppUser user) {
        return new AuthResponse(accessTokens.issue(user),
                new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name()));
    }

    private void validateRequestMarker(String requestMarker) {
        if (!"XMLHttpRequest".equals(requestMarker)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不允許的認證請求");
        }
    }

    private void setRefreshCookie(HttpServletResponse response, RefreshTokenService.IssuedRefreshToken token) {
        var maxAge = Duration.between(java.time.Instant.now(), token.expiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshCookie(token.value(), maxAge.isNegative() ? Duration.ZERO : maxAge).toString());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record CurrentUser(Long id, String email, String displayName, String role) {
    }

    public record AuthResponse(String accessToken, CurrentUser user) {
    }
}
