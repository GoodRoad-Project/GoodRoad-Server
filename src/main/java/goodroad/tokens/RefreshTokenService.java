package goodroad.tokens;

import goodroad.api.ApiErrors.ApiException;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepo userRepository;

    @Transactional
    public RefreshToken createRefreshToken(Long userId, String deviceInfo) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(generateSecureToken());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshToken.setDeviceInfo(deviceInfo);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        return createRefreshToken(userId, null);
    }

    private String generateSecureToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID().toString() + System.currentTimeMillis();
    }

    public Long validateAndGetUserId(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh token not found");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        return refreshToken.getUserId();
    }

    public RefreshToken validateAndGet(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh token not found");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeToken(token);
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    @Transactional
    public void revokeAllUserTokensByPhone(String phone) {
        UserEntity user = userRepository.findByPhoneHash(goodroad.security.Crypto.sha256Hex(
                        goodroad.security.Crypto.normPhone(phone)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    @Transactional
    public void deleteAllUserTokens(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public int cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        return 0;
    }

    public boolean hasActiveTokens(Long userId) {
        return refreshTokenRepository.hasValidToken(userId, LocalDateTime.now());
    }
}