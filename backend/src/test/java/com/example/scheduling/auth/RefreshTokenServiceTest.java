package com.example.scheduling.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.scheduling.user.AppUser;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class RefreshTokenServiceTest {
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(
            repository, Duration.ofDays(7), Duration.ofDays(30));
    private final AppUser user = new AppUser("amy@example.com", "hash", "Amy", AppUser.Role.EMPLOYEE);

    @Test
    void storesOnlyAHashAndRotatesTheRefreshToken() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var original = service.issue(user);

        var tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());
        var persistedOriginal = tokenCaptor.getValue();
        assertNotEquals(original.value(), persistedOriginal.getTokenHash());
        assertEquals(64, persistedOriginal.getTokenHash().length());

        when(repository.findByTokenHashForUpdate(persistedOriginal.getTokenHash()))
                .thenReturn(Optional.of(persistedOriginal));
        var rotated = service.rotate(original.value());

        assertSame(user, rotated.user());
        assertNotEquals(original.value(), rotated.refreshToken().value());
        assertNotNull(persistedOriginal.getRevokedAt());
        verify(repository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rejectsARefreshTokenAfterItHasBeenUsed() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var original = service.issue(user);
        var tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());
        var persistedOriginal = tokenCaptor.getValue();
        when(repository.findByTokenHashForUpdate(persistedOriginal.getTokenHash()))
                .thenReturn(Optional.of(persistedOriginal));

        service.rotate(original.value());

        var exception = assertThrows(ResponseStatusException.class, () -> service.rotate(original.value()));
        assertEquals(401, exception.getStatusCode().value());
        verify(repository).revokeFamily(eq(persistedOriginal.getFamilyId()), any(java.time.Instant.class));
    }
}
