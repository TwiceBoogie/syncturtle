package com.syncturtle.services.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.user.model.PasswordResetToken;

import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT passwordResetToken
            FROM PasswordResetToken passwordResetToken
            WHERE passwordResetToken.user.id = :userId
                AND passwordResetToken.consumedAt IS NULL
                AND passwordResetToken.invalidatedAt IS NULL
            """)
    List<PasswordResetToken> findOutstandingByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT passwordResetToken
            FROM PasswordResetToken passwordResetToken
            JOIN FETCH passwordResetToken.user user
            WHERE user.id = :userId
                AND passwordResetToken.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByUserIdAndTokenHashForUpdate(@Param("userId") UUID userId,
            @Param("tokenHash") String tokenHash);
}
