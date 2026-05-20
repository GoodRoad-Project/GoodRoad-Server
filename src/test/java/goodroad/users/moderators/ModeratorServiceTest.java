package goodroad.users.moderators;

import goodroad.model.Role;
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

    @Test
    void shouldCreateModerator() {
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        String id = service.createModerator("Анна", "Иванова", "+79990000003", "pass");

        assertEquals("10", id);
        verify(users).save(argThat(user ->
                Role.MODERATOR.name().equals(user.getRole()) && user.isActive()
        ));
    }

    @Test
    void shouldRejectDuplicatePhone() {
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(new UserEntity()));

        assertThrows(RuntimeException.class,
                () -> service.createModerator("Анна", "Иванова", "+79990000003", "pass"));
    }

    @Test
    void shouldDisableModerator() {
        UserEntity moderator = UserEntity.builder()
                .role(Role.MODERATOR.name())
                .active(true)
                .build();
        moderator.setId(10L);
        when(users.findById(10L)).thenReturn(Optional.of(moderator));

        service.disable("10");

        assertFalse(moderator.isActive());
        verify(users).save(moderator);
    }

    @Test
    void shouldNotDisableAdmin() {
        UserEntity admin = UserEntity.builder()
                .role(Role.MODERATOR_ADMIN.name())
                .active(true)
                .build();
        when(users.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class, () -> service.disable("1"));
    }

    @Test
    void shouldReturnModerators() {
        UserEntity moderator = UserEntity.builder()
                .firstName("Анна")
                .lastName("Иванова")
                .role(Role.MODERATOR.name())
                .active(true)
                .build();
        moderator.setId(10L);
        when(users.findByRoleIn(List.of(Role.MODERATOR.name(), Role.MODERATOR_ADMIN.name())))
                .thenReturn(List.of(moderator));

        List<ModeratorService.ModeratorView> result = service.getAllModerators();

        assertEquals(1, result.size());
        assertEquals("10", result.get(0).id());
        assertEquals(Role.MODERATOR.name(), result.get(0).role());
    }
}
