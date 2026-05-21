package goodroad.obstacle;

import goodroad.users.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserObstaclePolicyServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private UserObstaclePolicyRepo policies;

    @InjectMocks
    private UserObstaclePolicyService service;

    @Test
    void shouldReturnAllTypesWithSelectedFlags() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(policies.findByIdUserId(1L)).thenReturn(List.of(
                UserObstaclePolicyEntity.builder()
                        .id(new UserObstaclePolicyKey(1L, "STAIRS"))
                        .maxAllowedSeverity((short) 1)
                        .build()
        ));

        List<UserObstaclePolicyService.PolicyItem> result = service.getUserObstaclePolicies("+79990000001");

        assertEquals(6, result.size());
        assertTrue(result.stream().anyMatch(item ->
                item.obstacleType().equals("STAIRS") && item.selected() && item.maxAllowedSeverity() == 1
        ));
    }

    @Test
    void shouldReplaceSelectedPolicies() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(policies.findByIdUserId(1L)).thenReturn(List.of(
                UserObstaclePolicyEntity.builder()
                        .id(new UserObstaclePolicyKey(1L, "CURB"))
                        .maxAllowedSeverity((short) 2)
                        .build()
        ));

        UserObstaclePolicyService.ReplacePolicyReq req = new UserObstaclePolicyService.ReplacePolicyReq(List.of(
                new UserObstaclePolicyService.PolicyItem("CURB", true, (short) 2),
                new UserObstaclePolicyService.PolicyItem("STAIRS", false, (short) 1)
        ));

        List<UserObstaclePolicyService.PolicyItem> result = service.replaceUserObstaclePolicies("+79990000001", req);

        verify(policies).deleteByIdUserId(1L);
        verify(policies).save(any(UserObstaclePolicyEntity.class));
        verify(users).save(user);
        assertEquals(6, result.size());
    }

    @Test
    void shouldRejectInvalidSeverity() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));

        UserObstaclePolicyService.ReplacePolicyReq req = new UserObstaclePolicyService.ReplacePolicyReq(List.of(
                new UserObstaclePolicyService.PolicyItem("CURB", true, (short) 4)
        ));

        assertThrows(RuntimeException.class,
                () -> service.replaceUserObstaclePolicies("+79990000001", req));
    }
}
