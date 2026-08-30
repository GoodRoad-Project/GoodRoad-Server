package goodroad.users.users;

import goodroad.auth.AuthService;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.validation.TrustedUrlService;
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
class UserProfileServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private StorageService storageService;

    @Mock
    private TrustedUrlService trustedUrls;

    @InjectMocks
    private UserProfileService service;

    @Test
    void shouldGetCurrentUser() {
        UserEntity user = user(1L, Role.USER.name());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));

        UserProfileService.ProfileView view = service.getCurrentUser("+79990000001");

        assertEquals("1", view.id());
        assertEquals(Role.USER.name(), view.role());
        assertTrue(view.active());
    }

    @Test
    void shouldUpdateProfile() {
        UserEntity user = user(1L, Role.USER.name());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));

        UserProfileService.UpdateProfileReq req = new UserProfileService.UpdateProfileReq(
                "Мария", "Петрова", null
        );

        UserProfileService.ProfileView view = service.updateProfile("+79990000001", req);

        assertEquals("Мария", view.firstName());
        assertEquals("Петрова", view.lastName());
        verify(users).save(user);
    }

    @Test
    void shouldChangePhone() {
        UserEntity user = user(1L, Role.USER.name());
        user.setPassHash("hash");
        when(users.findByPhoneHashForUpdate(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.empty());

        UserProfileService.ChangePhoneReq req = new UserProfileService.ChangePhoneReq(
                "+79990000002", "pass"
        );

        UserProfileService.ProfileView view = service.changePhone("+79990000001", req);

        assertNotNull(view);
        verify(users).save(user);
    }

    @Test
    void shouldChangePasswordThroughAuthService() {
        service.changePassword(
                "+79990000001",
                new UserProfileService.ChangePasswordReq("old", "new")
        );

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

        UserProfileService.AvatarUploadResp resp = service.uploadAvatar("+79990000001", file);

        assertEquals("http://avatar", resp.photoUrl());
        assertEquals("http://avatar", user.getPhotoUrl());
        verify(users).save(user);
    }

    @Test
    void shouldDeleteCurrentUser() {
        UserEntity user = user(1L, Role.USER.name());
        user.setPassHash("hash");
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        service.deleteCurrent("+79990000001", new UserProfileService.DeleteAccountReq("pass"));

        verify(users).delete(user);
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