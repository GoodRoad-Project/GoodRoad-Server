package goodroad.auth;

import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
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

        AuthService.RegisterReq req = new AuthService.RegisterReq(
                "Ivan",
                "Ivanov",
                "+79990001122",
                "pass123"
        );

        AuthService.AuthResp resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("USER", resp.user().role());
        assertEquals("1", resp.user().id());

        verify(users).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowIfPhoneAlreadyExists() {

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(new UserEntity()));

        AuthService.RegisterReq req = new AuthService.RegisterReq(
                "Ivan",
                "Ivanov",
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

        AuthService.LoginReq req = new AuthService.LoginReq(
                "+79990001122",
                "pass123"
        );

        AuthService.AuthResp resp = authService.login(req);

        assertEquals("USER", resp.user().role());
        assertEquals("1", resp.user().id());

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
    void shouldChangePassword() {

        UserEntity user = UserEntity.builder()
                .passHash("oldHash")
                .active(true)
                .build();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("old", "oldHash"))
                .thenReturn(true);

        when(passwordEncoder.encode("new"))
                .thenReturn("newHash");

        when(users.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.ChangePassReq req =
                new AuthService.ChangePassReq("+79990001122", "old", "new");

        authService.changePass(req.phoneFromAuth(), req.oldPassword(), req.newPassword());

        verify(users).save(user);
        assertEquals("newHash", user.getPassHash());
    }
}