package goodroad.security;

import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepo users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserByValidBearerToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        Claims claims = Jwts.claims()
                .subject("+79990001122")
                .add("role", "USER")
                .build();

        UserEntity user = UserEntity.builder()
                .role("USER")
                .active(true)
                .passHash("hashed")
                .build();
        user.setId(10L);

        when(jwtService.parseClaims("valid-token")).thenReturn(claims);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990001122"))).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationAfterFilter = new AtomicReference<>();
        FilterChain chain = (req, resp) -> authenticationAfterFilter.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        Authentication authentication = authenticationAfterFilter.get();
        assertNotNull(authentication);
        assertEquals("79990001122", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void shouldNotAuthenticateWithoutBearerToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationAfterFilter = new AtomicReference<>();
        FilterChain chain = (req, resp) -> authenticationAfterFilter.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNull(authenticationAfterFilter.get());
        verifyNoInteractions(jwtService, users);
    }

    @Test
    void shouldNotAuthenticateInvalidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        when(jwtService.parseClaims("invalid-token")).thenThrow(new RuntimeException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationAfterFilter = new AtomicReference<>();
        FilterChain chain = (req, resp) -> authenticationAfterFilter.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNull(authenticationAfterFilter.get());
        verifyNoInteractions(users);
    }

    @Test
    void shouldNotAuthenticateInactiveUser() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        Claims claims = Jwts.claims()
                .subject("+79990001122")
                .add("role", "USER")
                .build();

        UserEntity user = UserEntity.builder()
                .role("USER")
                .active(false)
                .passHash("hashed")
                .build();

        when(jwtService.parseClaims("valid-token")).thenReturn(claims);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990001122"))).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationAfterFilter = new AtomicReference<>();
        FilterChain chain = (req, resp) -> authenticationAfterFilter.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNull(authenticationAfterFilter.get());
    }

    @Test
    void shouldNotAuthenticateWhenTokenRoleDoesNotMatchCurrentUserRole() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        Claims claims = Jwts.claims()
                .subject("+79990001122")
                .add("role", "USER")
                .build();

        UserEntity user = UserEntity.builder()
                .role("MODERATOR")
                .active(true)
                .passHash("hashed")
                .build();

        when(jwtService.parseClaims("valid-token")).thenReturn(claims);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990001122"))).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationAfterFilter = new AtomicReference<>();
        FilterChain chain = (req, resp) -> authenticationAfterFilter.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNull(authenticationAfterFilter.get());
    }
}
