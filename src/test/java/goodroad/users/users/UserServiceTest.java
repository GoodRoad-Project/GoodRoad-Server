package goodroad.users.users;

import goodroad.api.ApiErrors.ApiException;
import goodroad.auth.AuthService;
import goodroad.security.Crypto;
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
class UserSettingsServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserSettingsService service;

    private UserEntity user() {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setFirstName("Ivan");
        u.setLastName("Ivanov");
        u.setPhoneHash(Crypto.sha256Hex("+79990001122"));
        u.setRole("USER");
        u.setActive(true);
        u.setPassHash("hashed");
        return u;
    }

    @Test
    void shouldGetCurrentUser() {
        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user()));

        var result = service.getCurrentUser("+79990001122");

        assertEquals("1", result.id());
        assertEquals("USER", result.role());
    }

    @Test
    void shouldUpdateFirstName() {
        UserEntity user = user();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        var req = new UserSettingsService.UpdateSettingsReq(
                "NewName",
                null,
                null,
                null
        );

        var result = service.updateCurrentUserSettings("+79990001122", req);

        assertEquals("NewName", user.getFirstName());
        verify(users).save(user);
        assertEquals("1", result.id());
    }

    @Test
    void shouldThrowWhenEmptyUpdate() {
        var req = new UserSettingsService.UpdateSettingsReq(
                null, null, null, null
        );

        assertThrows(ApiException.class,
                () -> service.updateCurrentUserSettings("+79990001122", req));
    }

    @Test
    void shouldUpdatePhone() {
        UserEntity user = user();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        var req = new UserSettingsService.UpdateSettingsReq(
                null,
                null,
                null,
                "+79990003333"
        );

        service.updateCurrentUserSettings("+79990001122", req);

        verify(users).save(user);
    }

    @Test
    void shouldCallAuthServiceChangePassword() {
        service.changePassword("+79990001122", "old", "new");
        verify(authService).changePass("+79990001122", "old", "new");
    }

    @Test
    void shouldDeleteUser() {
        UserEntity user = user();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("pass", "hashed"))
                .thenReturn(true);

        var req = new UserSettingsService.DeleteAccountReq("pass");

        service.deleteCurrent("+79990001122", req);

        verify(users).delete(user);
    }

    @Test
    void shouldThrowIfWrongPassword() {
        UserEntity user = user();

        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        var req = new UserSettingsService.DeleteAccountReq("wrong");

        assertThrows(ApiException.class,
                () -> service.deleteCurrent("+79990001122", req));
    }
}