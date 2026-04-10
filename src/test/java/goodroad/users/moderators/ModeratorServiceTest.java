package goodroad.users.moderators;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModeratorServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ModeratorService service;

    private UserEntity moderator() {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setFirstName("Ivan");
        u.setLastName("Ivanov");
        u.setPhoneHash(Crypto.sha256Hex("+79990001122"));
        u.setRole(Role.MODERATOR.name());
        u.setActive(true);
        return u;
    }

    @Test
    void shouldCreateModerator() {
        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("pass"))
                .thenReturn("hashed");

        when(users.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity u = invocation.getArgument(0);
                    u.setId(1L);
                    return u;
                });

        String id = service.create("Ivan", "Ivanov", "+79990001122", "pass");

        assertEquals("1", id);
        verify(users).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowIfPhoneExists() {
        when(users.findByPhoneHash(anyString()))
                .thenReturn(Optional.of(new UserEntity()));

        assertThrows(ApiException.class,
                () -> service.create("Ivan", "Ivanov", "+79990001122", "pass"));
    }

    @Test
    void shouldDisableModerator() {
        UserEntity user = moderator();

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        service.disable("1");

        assertFalse(user.isActive());
        verify(users).save(user);
    }

    @Test
    void shouldThrowIfDisableAdmin() {
        UserEntity user = moderator();
        user.setRole(Role.MODERATOR_ADMIN.name());

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(ApiException.class,
                () -> service.disable("1"));
    }

    @Test
    void shouldGetAllModerators() {
        UserEntity user = moderator();

        when(users.findByRoleIn(List.of(Role.MODERATOR.name(), Role.MODERATOR_ADMIN.name())))
                .thenReturn(List.of(user));

        var result = service.getAllModerators();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals(Role.MODERATOR.name(), result.get(0).role());
    }
}