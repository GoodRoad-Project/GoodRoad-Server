package goodroad.security;

import goodroad.users.repository.UserEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void shouldGenerateAndParseToken() {
        JwtService jwtService = new JwtService(SECRET, 30);

        UserEntity user = UserEntity.builder()
                .role("USER")
                .active(true)
                .passHash("hashed")
                .build();
        user.setId(42L);

        String token = jwtService.generateToken("+79990001122", user);
        Claims claims = jwtService.parseClaims(token);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("+79990001122", claims.getSubject());
        assertEquals(42L, ((Number) claims.get("userId")).longValue());
        assertEquals("USER", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(SECRET, 30);
        JwtService verifier = new JwtService("fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210", 30);

        UserEntity user = UserEntity.builder()
                .role("USER")
                .active(true)
                .passHash("hashed")
                .build();
        user.setId(42L);

        String token = issuer.generateToken("+79990001122", user);

        assertThrows(RuntimeException.class, () -> verifier.parseClaims(token));
    }

    @Test
    void shouldRejectShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short", 30));
    }
}
