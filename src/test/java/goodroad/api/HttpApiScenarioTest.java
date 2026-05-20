package goodroad.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import goodroad.auth.AuthController;
import goodroad.auth.AuthService;
import goodroad.controller.RouteController;
import goodroad.model.PathResponse;
import goodroad.model.ResponseInfo;
import goodroad.model.RouteResponse;
import goodroad.obstacle.ObstacleController;
import goodroad.obstacle.ObstacleDBService;
import goodroad.obstacle.UserObstaclePolicyController;
import goodroad.obstacle.UserObstaclePolicyService;
import goodroad.reviews.ReviewModerationController;
import goodroad.reviews.ReviewModerationService;
import goodroad.reviews.UserReviewController;
import goodroad.reviews.UserReviewService;
import goodroad.service.RouteService;
import goodroad.users.moderators.ModeratorController;
import goodroad.users.moderators.ModeratorService;
import goodroad.users.users.UserController;
import goodroad.users.users.UserSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class HttpApiScenarioTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AuthService authService;

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private ModeratorService moderatorService;

    @Mock
    private UserObstaclePolicyService policyService;

    @Mock
    private UserReviewService userReviewService;

    @Mock
    private ReviewModerationService moderationService;

    @Mock
    private ObstacleDBService obstacleService;

    @Mock
    private RouteService routeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterLoginAndRecoverPassword() throws Exception {
        MockMvc mvc = standaloneSetup(new AuthController(authService)).build();

        when(authService.register(any(AuthService.RegisterReq.class)))
                .thenReturn(new AuthService.AuthResp(new AuthService.UserView("10", "USER")));
        when(authService.login(any(AuthService.LoginReq.class)))
                .thenReturn(new AuthService.AuthResp(new AuthService.UserView("10", "USER")));

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Иван",
                                  "lastName": "Петров",
                                  "phone": "+79990000001",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("10"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "+79990000001",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("10"));

        mvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "+79990000001",
                                  "firstName": "Иван",
                                  "lastName": "Петров",
                                  "newPassword": "123"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authService).recoverPass("+79990000001", "Иван", "Петров", "123");
    }

    @Test
    void shouldUseUserEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new UserController(userSettingsService)).build();
        setCurrentUser("+79990000001");

        when(userSettingsService.getCurrentUser("+79990000001"))
                .thenReturn(new UserSettingsService.SettingsView(
                        "10", "USER", "Иван", "Петров", null, true
                ));
        when(userSettingsService.updateCurrentUserSettings(eq("+79990000001"), any(UserSettingsService.UpdateSettingsReq.class)))
                .thenReturn(new UserSettingsService.SettingsView(
                        "10", "USER", "Иван", "Иванов", null, true
                ));
        when(userSettingsService.uploadAvatar(eq("+79990000001"), any()))
                .thenReturn(new UserSettingsService.AvatarUploadResp("https://storage/avatar.png"));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("10"))
                .andExpect(jsonPath("$.firstName").value("Иван"));

        mvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Иван",
                                  "lastName": "Иванов",
                                  "photoUrl": null,
                                  "phone": "+79990000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Иванов"));

        mvc.perform(post("/users")
                        .param("oldPassword", "123")
                        .param("newPassword", "1234"))
                .andExpect(status().isOk());
        verify(userSettingsService).changePassword("+79990000001", "123", "1234");

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );
        mvc.perform(multipart("/users/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://storage/avatar.png"));

        mvc.perform(delete("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isOk());
        verify(userSettingsService).deleteCurrent(eq("+79990000001"), any(UserSettingsService.DeleteAccountReq.class));

        mvc.perform(delete("/users/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "admin"
                                }
                                """))
                .andExpect(status().isOk());
        verify(userSettingsService).deleteByAdmin(eq("+79990000001"), eq("11"), any(UserSettingsService.DeleteAccountReq.class));
    }

    @Test
    void shouldUseModeratorEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new ModeratorController(moderatorService)).build();

        when(moderatorService.createModerator("Анна", "Сидорова", "+79990000151", "123"))
                .thenReturn("51");
        when(moderatorService.getAllModerators())
                .thenReturn(List.of(new ModeratorService.ModeratorView(
                        "51", "MODERATOR", "Анна", "Сидорова", null, true
                )));

        mvc.perform(post("/users/moderators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Анна",
                                  "lastName": "Сидорова",
                                  "phone": "+79990000151",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("51"));

        mvc.perform(get("/users/moderators/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("51"))
                .andExpect(jsonPath("$[0].role").value("MODERATOR"));

        mvc.perform(put("/users/moderators/51"))
                .andExpect(status().isOk());
        verify(moderatorService).disable("51");
    }

    @Test
    void shouldUseObstaclePolicyEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new UserObstaclePolicyController(policyService)).build();
        Principal principal = principal("+79990000001");

        List<UserObstaclePolicyService.PolicyItem> policies = List.of(
                new UserObstaclePolicyService.PolicyItem("STAIRS", true, (short) 1),
                new UserObstaclePolicyService.PolicyItem("CURB", false, null)
        );
        when(policyService.getUserObstaclePolicies("+79990000001")).thenReturn(policies);
        when(policyService.replaceUserObstaclePolicies(eq("+79990000001"), any(UserObstaclePolicyService.ReplacePolicyReq.class)))
                .thenReturn(policies);

        mvc.perform(get("/users/obstacles").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].obstacleType").value("STAIRS"))
                .andExpect(jsonPath("$[0].selected").value(true));

        mvc.perform(put("/users/obstacles")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "obstacleType": "STAIRS",
                                      "selected": true,
                                      "maxAllowedSeverity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].obstacleType").value("STAIRS"));
    }

    @Test
    void shouldUseReviewEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new UserReviewController(userReviewService)).build();
        Principal principal = principal("+79990000001");

        UserReviewService.ReviewCardResp review = reviewCard("90", "201", "PENDING");
        when(userReviewService.createReview(eq("+79990000001"), any(UserReviewService.UpsertReviewReq.class)))
                .thenReturn(review);
        when(userReviewService.listOwnReviews("+79990000001")).thenReturn(List.of(review));
        when(userReviewService.getOwnReviewPoints("+79990000001"))
                .thenReturn(new UserReviewService.ReviewPointsResp(15, 2));
        when(userReviewService.updateOwnReview(eq("+79990000001"), eq("90"), any(UserReviewService.UpsertReviewReq.class)))
                .thenReturn(reviewCard("90", "201", "PENDING"));
        when(userReviewService.uploadReviewPhoto(eq("+79990000001"), any()))
                .thenReturn(new UserReviewService.ReviewPhotoUploadResp("https://storage/review.png"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "review.png", "image/png", new byte[]{1, 2, 3}
        );
        mvc.perform(multipart("/reviews/photos").file(file).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://storage/review.png"));

        mvc.perform(post("/reviews")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("90"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(get("/reviews/own").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("90"));

        mvc.perform(get("/reviews/points").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(15))
                .andExpect(jsonPath("$.approvedReviews").value(2));

        mvc.perform(patch("/reviews/90")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("90"));

        mvc.perform(delete("/reviews/90").principal(principal))
                .andExpect(status().isOk());
        verify(userReviewService).deleteOwnReview("+79990000001", "90");
    }

    @Test
    void shouldUseModerationEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new ReviewModerationController(moderationService)).build();
        Principal principal = principal("+79990000151");

        ReviewModerationService.ReviewView review = moderationReview("90", "201");
        when(moderationService.listPending("+79990000151", 0, 20))
                .thenReturn(new ReviewModerationService.ReviewsPageResp(List.of(review), 0, 20, 1));
        when(moderationService.takeInWork("+79990000151", "90")).thenReturn(review);

        mvc.perform(get("/reviews/moderation/pending")
                        .principal(principal)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("90"));

        mvc.perform(post("/reviews/moderation/90/take").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("90"));

        mvc.perform(post("/reviews/moderation/90/approve").principal(principal))
                .andExpect(status().isOk());
        verify(moderationService).approve("+79990000151", "90");

        mvc.perform(post("/reviews/moderation/90/reject")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Недостаточно данных"
                                }
                                """))
                .andExpect(status().isOk());
        verify(moderationService).reject("+79990000151", "90", "Недостаточно данных");

        mvc.perform(post("/reviews/moderation/90/release").principal(principal))
                .andExpect(status().isOk());
        verify(moderationService).release("+79990000151", "90");
    }

    @Test
    void shouldUseObstacleAndRouteEndpoints() throws Exception {
        MockMvc obstacleMvc = standaloneSetup(new ObstacleController(obstacleService)).build();
        MockMvc routeMvc = standaloneSetup(new RouteController(routeService)).build();

        ObstacleDBService.ObstacleMapItemResp obstacle = new ObstacleDBService.ObstacleMapItemResp(
                "201",
                "STAIRS",
                59.9300,
                30.3300,
                obstacleAddress(),
                (short) 2,
                Map.of("STAIRS", (short) 3),
                4,
                Instant.parse("2026-05-01T10:00:00Z")
        );
        when(obstacleService.listInBox(59.92, 59.95, 30.30, 30.35)).thenReturn(List.of(obstacle));
        when(obstacleService.getCard("201"))
                .thenReturn(new ObstacleDBService.ObstacleCardResp(
                        "201",
                        "STAIRS",
                        59.9300,
                        30.3300,
                        obstacleAddress(),
                        (short) 2,
                        4,
                        Instant.parse("2026-05-01T10:00:00Z"),
                        List.of()
                ));
        when(routeService.buildThreeRoutes(any()))
                .thenReturn(new RouteResponse("route-1", List.of(
                        new PathResponse(1200.0, 900000L, true, "encoded", List.of(), "inclusive")
                ), new ResponseInfo()));

        obstacleMvc.perform(get("/obstacles")
                        .param("minLat", "59.9200")
                        .param("maxLat", "59.9500")
                        .param("minLon", "30.3000")
                        .param("maxLon", "30.3500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("201"))
                .andExpect(jsonPath("$[0].type").value("STAIRS"));

        obstacleMvc.perform(get("/obstacles/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("201"));

        routeMvc.perform(get("/routes/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.service").value("route-service"));

        routeMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "start": "59.9300,30.3300",
                                  "end": "59.9400,30.3400",
                                  "obstacle_policies": [
                                    {
                                      "obstacle_type": "STAIRS",
                                      "max_allowed_severity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("route-1"))
                .andExpect(jsonPath("$.paths[0].route_type").value("inclusive"));
    }

    private void setCurrentUser(String phone) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(phone, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Principal principal(String phone) {
        return new UsernamePasswordAuthenticationToken(phone, "password");
    }

    private UserReviewService.ReviewCardResp reviewCard(String id, String featureId, String status) {
        return new UserReviewService.ReviewCardResp(
                id,
                featureId,
                new UserReviewService.AddressReq(
                        "Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "10", null
                ),
                59.9300,
                30.3300,
                (short) 4,
                List.of(new UserReviewService.ObstacleSeverityItem("STAIRS", (short) 3)),
                "Есть лестница",
                List.of("https://storage/review.png"),
                status,
                Instant.parse("2026-05-01T10:00:00Z"),
                0,
                null
        );
    }

    private ReviewModerationService.ReviewView moderationReview(String id, String featureId) {
        return new ReviewModerationService.ReviewView(
                id,
                featureId,
                "10",
                new ReviewModerationService.AddressResp(
                        "Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "10", null
                ),
                59.9300,
                30.3300,
                (short) 4,
                List.of(new ReviewModerationService.ObstacleItemResp("STAIRS", (short) 3)),
                "Есть лестница",
                List.of("https://storage/review.png"),
                "PENDING",
                Instant.parse("2026-05-01T10:00:00Z"),
                true,
                true,
                "51",
                Instant.parse("2026-05-01T11:00:00Z"),
                null
        );
    }

    private ObstacleDBService.AddressResp obstacleAddress() {
        return new ObstacleDBService.AddressResp(
                "Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "10", null
        );
    }

    private String reviewJson() {
        return """
                {
                  "latitude": 59.9300,
                  "longitude": 30.3300,
                  "address": {
                    "country": "Россия",
                    "region": "Санкт-Петербург",
                    "localityType": "город",
                    "city": "Санкт-Петербург",
                    "street": "Садовая",
                    "house": "10"
                  },
                  "rating": 4,
                  "obstacles": [
                    {
                      "obstacleType": "STAIRS",
                      "severity": 3
                    }
                  ],
                  "comment": "Есть лестница",
                  "photoUrls": [
                    "https://storage/review.png"
                  ]
                }
                """;
    }
}
