package goodroad.auth;

import goodroad.users.repository.UserEntity;
import goodroad.security.JwtService;
import goodroad.users.repository.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUser() {

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("pass123"))
                .thenReturn("hashedPass");

        when(users.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity u = invocation.getArgument(0);
                    u.setId(1L);
                    return u;
                });
        when(jwtService.generateAccessToken(anyString(), any(UserEntity.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), any(UserEntity.class))).thenReturn("refresh-token");

        AuthService.RegisterReq req = new AuthService.RegisterReq(
                "Иван",
                "Иванов",
                "+79990001122",
                "pass123"
        );

        AuthService.AuthResp resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("USER", resp.user().role());
        assertEquals("1", resp.user().id());
        assertEquals("access-token", resp.accessToken());
        assertEquals("refresh-token", resp.refreshToken());
        assertEquals("Bearer", resp.tokenType());

        verify(users).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowIfPhoneAlreadyExists() {

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(new UserEntity()));

        AuthService.RegisterReq req = new AuthService.RegisterReq(
                "Иван",
                "Иванов",
                "+79990001122",
                "pass123"
        );

        assertThrows(RuntimeException.class,
                () -> authService.register(req));

    }

    @Test
    void shouldLoginUser() {

        UserEntity user = UserEntity.builder()
                .passHash("hashed")
                .active(true)
                .role("USER")
                .build();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("pass123", "hashed"))
                .thenReturn(true);

        when(users.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity u = invocation.getArgument(0);
                    u.setId(1L);
                    return u;
                });
        when(jwtService.generateAccessToken(anyString(), any(UserEntity.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), any(UserEntity.class))).thenReturn("refresh-token");

        AuthService.LoginReq req = new AuthService.LoginReq(
                "+79990001122",
                "pass123"
        );

        AuthService.AuthResp resp = authService.login(req);

        assertEquals("USER", resp.user().role());
        assertEquals("1", resp.user().id());
        assertEquals("access-token", resp.accessToken());
        assertEquals("refresh-token", resp.refreshToken());
        assertEquals("Bearer", resp.tokenType());

        verify(users).save(user);
    }

    @Test
    void shouldFailLoginWrongPassword() {

        UserEntity user = UserEntity.builder()
                .passHash("hashed")
                .active(true)
                .role("USER")
                .build();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        AuthService.LoginReq req = new AuthService.LoginReq(
                "+79990001122",
                "wrong"
        );

        assertThrows(RuntimeException.class,
                () -> authService.login(req));

    }


    @Test
    void shouldRefreshTokensAndReturnCurrentRole() {
        Claims claims = Jwts.claims()
                .subject("+79990001122")
                .add("tokenType", "REFRESH")
                .build();

        UserEntity user = UserEntity.builder()
                .passHash("hashed")
                .active(true)
                .role("VOLUNTEER")
                .build();
        user.setId(1L);

        when(jwtService.parseClaims("refresh-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(users.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(anyString(), any(UserEntity.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(anyString(), any(UserEntity.class))).thenReturn("new-refresh-token");

        AuthService.AuthResp resp = authService.refresh(new AuthService.RefreshReq("refresh-token"));

        assertEquals("VOLUNTEER", resp.user().role());
        assertEquals("1", resp.user().id());
        assertEquals("new-access-token", resp.accessToken());
        assertEquals("new-refresh-token", resp.refreshToken());
        assertEquals("Bearer", resp.tokenType());
        verify(users).save(user);
    }

    @Test
    void shouldRejectAccessTokenAsRefreshToken() {
        Claims claims = Jwts.claims()
                .subject("+79990001122")
                .add("tokenType", "ACCESS")
                .build();

        when(jwtService.parseClaims("access-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.refresh(new AuthService.RefreshReq("access-token")));

        verifyNoInteractions(passwordEncoder);
        verify(users, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldRecoverPassword() {

        UserEntity user = UserEntity.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .passHash("oldHash")
                .role("USER")
                .active(true)
                .build();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode("new"))
                .thenReturn("newHash");

        authService.recoverPass("+79990001122", "Иван", "Иванов", "new");

        verify(users).save(user);
        assertEquals("newHash", user.getPassHash());
    }

    @Test
    void shouldFailRecoverPasswordForWrongName() {

        UserEntity user = UserEntity.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .passHash("oldHash")
                .role("USER")
                .active(true)
                .build();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> authService.recoverPass("+79990001122", "Петр", "Иванов", "new"));

        verify(users, never()).save(any(UserEntity.class));
    }
}