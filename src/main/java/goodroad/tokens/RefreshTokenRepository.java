package goodroad.tokens;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false")
    java.util.List<RefreshToken> findAllValidByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    boolean hasValidToken(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    java.util.List<RefreshToken> findAllExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    void revokeToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId IN :userIds")
    void deleteAllByUserIds(@Param("userIds") java.util.Collection<Long> userIds);
}