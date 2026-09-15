package com.example.scheduling.auth;

import com.example.scheduling.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private RefreshToken replacedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(AppUser user, String tokenHash, Instant expiresAt, UUID familyId,
            Instant absoluteExpiresAt, Instant createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.familyId = familyId;
        this.absoluteExpiresAt = absoluteExpiresAt;
        this.createdAt = createdAt;
    }

    public AppUser getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    public RefreshToken getReplacedBy() {
        return replacedBy;
    }

    public boolean isUsableAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant) && absoluteExpiresAt.isAfter(instant);
    }

    public void revoke(Instant instant) {
        if (revokedAt == null) {
            revokedAt = instant;
        }
    }

    public void replaceWith(RefreshToken replacement, Instant instant) {
        revoke(instant);
        replacedBy = replacement;
    }
}
