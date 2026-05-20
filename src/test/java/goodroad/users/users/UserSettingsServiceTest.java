package goodroad.users.users;

import goodroad.auth.AuthService;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserSettingsService service;

    @Test
    void shouldGetCurrentUser() {
        UserEntity user = user(1L, Role.USER.name());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));

        UserSettingsService.SettingsView view = service.getCurrentUser("+79990000001");

        assertEquals("1", view.id());
        assertEquals(Role.USER.name(), view.role());
        assertTrue(view.active());
    }

    @Test
    void shouldUpdateCurrentUser() {
        UserEntity user = user(1L, Role.USER.name());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));

        UserSettingsService.UpdateSettingsReq req = new UserSettingsService.UpdateSettingsReq(
                "Мария", "Петрова", "http://photo", "+79990000002"
        );

        UserSettingsService.SettingsView view = service.updateCurrentUserSettings("+79990000001", req);

        assertEquals("Мария", view.firstName());
        assertEquals("Петрова", view.lastName());
        assertEquals(Crypto.sha256Hex("79990000002"), user.getPhoneHash());
        verify(users).save(user);
    }

    @Test
    void shouldChangePasswordThroughAuthService() {
        service.changePassword("+79990000001", "old", "new");

        verify(authService).changePass("+79990000001", "old", "new");
    }

    @Test
    void shouldUploadAvatar() {
        UserEntity user = user(1L, Role.USER.name());
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[] {1, 2, 3}
        );
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(storageService.uploadAvatar(file, "1")).thenReturn("http://avatar");

        UserSettingsService.AvatarUploadResp resp = service.uploadAvatar("+79990000001", file);

        assertEquals("http://avatar", resp.photoUrl());
        assertEquals("http://avatar", user.getPhotoUrl());
        verify(users).save(user);
    }

    @Test
    void shouldRejectBadAvatarType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.txt", "text/plain", new byte[] {1}
        );

        assertThrows(RuntimeException.class,
                () -> service.uploadAvatar("+79990000001", file));
    }

    @Test
    void shouldDeleteCurrentUser() {
        UserEntity user = user(1L, Role.USER.name());
        user.setPassHash("hash");
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        service.deleteCurrent("+79990000001", new UserSettingsService.DeleteAccountReq("pass"));

        verify(users).delete(user);
    }

    @Test
    void shouldDeleteUserByAdmin() {
        UserEntity admin = user(1L, Role.MODERATOR_ADMIN.name());
        admin.setPassHash("hash");
        UserEntity target = user(2L, Role.USER.name());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin", "hash")).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(target));

        service.deleteByAdmin("+79990000003", "2", new UserSettingsService.DeleteAccountReq("admin"));

        verify(users).delete(target);
    }

    private UserEntity user(Long id, String role) {
        UserEntity user = UserEntity.builder()
                .firstName("Анна")
                .lastName("Иванова")
                .phoneHash(Crypto.sha256Hex("79990000001"))
                .role(role)
                .active(true)
                .build();
        user.setId(id);
        return user;
    }
}
