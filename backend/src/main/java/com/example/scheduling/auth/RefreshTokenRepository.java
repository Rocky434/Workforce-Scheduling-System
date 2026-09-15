package com.example.scheduling.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
        @Query("select token.familyId from RefreshToken token where token.tokenHash = :tokenHash")
        Optional<UUID> findFamilyIdByTokenHash(@Param("tokenHash") String tokenHash);

        @Query(value = "select pg_advisory_xact_lock(hashtextextended(cast(:familyId as text), 0))", nativeQuery = true)
        void lockFamily(@Param("familyId") UUID familyId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select token from RefreshToken token join fetch token.user where token.tokenHash = :tokenHash")
        Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

        @Modifying
        @Query("update RefreshToken token set token.revokedAt = :revokedAt "
                        + "where token.familyId = :familyId and token.revokedAt is null")
        int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);
}
