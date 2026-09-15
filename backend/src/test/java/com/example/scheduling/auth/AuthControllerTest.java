package com.example.scheduling.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.scheduling.user.AppUser;
import com.example.scheduling.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AccessTokenService accessTokens = mock(AccessTokenService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final AuthController controller = new AuthController(users, passwords, accessTokens, refreshTokens, false);
    private final AppUser user = new AppUser("amy@example.com", "hash", "Amy", AppUser.Role.EMPLOYEE);

    @Test
    void loginReturnsAnAccessTokenAndSetsAnHttpOnlyRefreshCookie() {
        when(users.findByEmailIgnoreCase("amy@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("password", "hash")).thenReturn(true);
        when(accessTokens.issue(user)).thenReturn("access-token");
        when(refreshTokens.issue(user)).thenReturn(
                new RefreshTokenService.IssuedRefreshToken(
                        "refresh-token", Instant.now().plus(Duration.ofDays(7)), null));
        var response = new MockHttpServletResponse();

        var body = controller.login(
                new AuthController.LoginRequest("amy@example.com", "password"), response, "XMLHttpRequest");

        assertEquals("access-token", body.accessToken());
        var cookie = response.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("refresh_token=refresh-token"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/api/auth"));
    }

    @Test
    void refreshRotatesTheCookieAndLogoutRevokesIt() {
        var replacement = new RefreshTokenService.IssuedRefreshToken(
                "replacement", Instant.now().plus(Duration.ofDays(7)), null);
        when(refreshTokens.rotate("original"))
                .thenReturn(new RefreshTokenService.RefreshSession(user, replacement));
        when(accessTokens.issue(user)).thenReturn("new-access-token");
        var refreshResponse = new MockHttpServletResponse();

        var body = controller.refresh("original", refreshResponse, "XMLHttpRequest");

        assertEquals("new-access-token", body.accessToken());
        assertTrue(refreshResponse.getHeader("Set-Cookie").contains("refresh_token=replacement"));

        var logoutResponse = new MockHttpServletResponse();
        controller.logout("replacement", logoutResponse, "XMLHttpRequest");
        verify(refreshTokens).revoke("replacement");
        assertTrue(logoutResponse.getHeader("Set-Cookie").contains("Max-Age=0"));
    }
}
