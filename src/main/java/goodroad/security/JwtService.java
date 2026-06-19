package goodroad.security;

import goodroad.users.repository.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    @Autowired
    public JwtService(
            @Value("${app.jwt.secret:goodroad-dev-jwt-secret-change-me-please-32-symbols-minimum}") String secret,
            @Value("${app.jwt.ttl-days:30}") long ttlDays,
            @Value("${app.jwt.refresh-ttl-days:60}") long refreshTtlDays
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofDays(ttlDays);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    public JwtService(String secret, long ttlDays) {
        this(secret, ttlDays, 60);
    }

    public String generateToken(String phoneNorm, UserEntity user) {
        return generateAccessToken(phoneNorm, user);
    }

    public String generateAccessToken(String phoneNorm, UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(phoneNorm)
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String phoneNorm, UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(phoneNorm)
                .claim("userId", user.getId())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }
}
