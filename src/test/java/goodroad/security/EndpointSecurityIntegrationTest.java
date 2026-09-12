package goodroad.security;

import goodroad.api.ApiErrors;
import goodroad.auth.AuthController;
import goodroad.auth.AuthService;
import goodroad.controller.RouteController;
import goodroad.points.PointLedgerService;
import goodroad.rewards.RewardController;
import goodroad.rewards.RewardService;
import goodroad.service.RouteService;
import goodroad.tasks.TaskController;
import goodroad.tasks.TaskService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.users.users.UserController;
import goodroad.users.users.UserProfileService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        RouteController.class,
        TaskController.class,
        RewardController.class,
        UserController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiErrors.GlobalHandler.class})
class EndpointSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;
    @MockitoBean AuthService authService;
    @MockitoBean UserRepo users;
    @MockitoBean RouteService routeService;
    @MockitoBean TaskService taskService;
    @MockitoBean RewardService rewardService;
    @MockitoBean PointLedgerService pointLedgerService;
    @MockitoBean UserProfileService userProfileService;

    @Test
    void leavesOnlyHealthAndAuthenticationEntryPointsPublic() throws Exception {
        mvc.perform(get("/routes/health"))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "+79990000001",
                                  "firstName": "Иван",
                                  "lastName": "Петров",
                                  "newPassword": "newPassword"
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\":\"59.93,30.31\",\"end\":\"59.94,30.32\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mvc.perform(post("/tasks/generation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":59.93,\"longitude\":30.31}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(multipart("/users/avatar")
                        .file("file", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsAccessTokenButRejectsMalformedJsonWithUniformError() throws Exception {
        authenticate("access-token", "USER", "ACCESS");

        mvc.perform(post("/tasks/generation")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.fields").doesNotExist())
                .andExpect(jsonPath("$.ts").exists());
    }

    @Test
    void refusesRefreshTokenAtProtectedEndpoint() throws Exception {
        authenticate("refresh-token", "USER", "REFRESH");

        mvc.perform(post("/tasks/generation")
                        .header("Authorization", "Bearer refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":59.93,\"longitude\":30.31}"))
                .andExpect(status().isUnauthorized());

        verify(taskService, never()).generateForCurrentLocation(anyString(), any());
    }

    @Test
    void forbidsRegularUserFromInventoryAdministration() throws Exception {
        authenticate("user-token", "USER", "ACCESS");

        mvc.perform(post("/rewards/admin/offers/1/inventory")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"CODE-1\"}]"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(rewardService, never()).addInventory(anyString(), any());
    }

    private void authenticate(String token, String role, String tokenType) {
        Claims claims = Jwts.claims()
                .subject("+79990000001")
                .add("role", role)
                .add("tokenType", tokenType)
                .build();
        UserEntity user = UserEntity.builder()
                .role(role)
                .active(true)
                .createdAt(Instant.now())
                .build();
        user.setId(10L);

        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn("ACCESS".equals(tokenType));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
    }
}
