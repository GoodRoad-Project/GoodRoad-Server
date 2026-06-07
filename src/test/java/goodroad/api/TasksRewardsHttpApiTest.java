package goodroad.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import goodroad.rewards.*;
import goodroad.tasks.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.test.web.servlet.*;
import java.security.*;
import java.time.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@ExtendWith(MockitoExtension.class)
class TasksRewardsHttpApiTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    TaskService taskService;

    @Mock
    RewardService rewardService;

    @Test
    void shouldUseTaskRoutes() throws Exception {
        MockMvc mvc = standaloneSetup(new TaskController(taskService)).build();

        when(taskService.feed("+79990000001", "REVIEW", 59.93, 30.31)).thenReturn(List.of(
                new TaskService.TaskView(
                        "10",
                        "REVIEW",
                        "Оцените 3 точки",
                        30,
                        3,
                        1,
                        true,
                        List.of(new TaskService.TargetView(
                                "1",
                                "OBSTACLE_FEATURE",
                                "100",
                                "Садовая, 1",
                                59.93,
                                30.31,
                                true
                        ))
                )
        ));

        when(taskService.completed("+79990000001")).thenReturn(List.of(
                new TaskService.CompletedTaskView(
                        "5",
                        "10",
                        30,
                        Instant.parse("2026-06-01T10:00:00Z")
                )
        ));

        mvc.perform(get("/tasks")
                        .principal(principal("+79990000001"))
                        .param("activityType", "REVIEW")
                        .param("latitude", "59.93")
                        .param("longitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("10"))
                .andExpect(jsonPath("$[0].points").value(30))
                .andExpect(jsonPath("$[0].targets[0].done").value(true));

        mvc.perform(get("/tasks/completed")
                        .principal(principal("+79990000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pointsAwarded").value(30));

        mvc.perform(post("/tasks")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityType": "REVIEW",
                                  "title": "Оцените три точки",
                                  "targetCount": 3,
                                  "targets": [
                                    {"targetType":"OBSTACLE_FEATURE","targetId":1,"title":"А"},
                                    {"targetType":"OBSTACLE_FEATURE","targetId":2,"title":"Б"},
                                    {"targetType":"OBSTACLE_FEATURE","targetId":3,"title":"В"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        verify(taskService).createTask(any(TaskService.TaskCreateReq.class));
    }

    @Test
    void shouldUseRewardRoutes() throws Exception {
        MockMvc mvc = standaloneSetup(new RewardController(rewardService)).build();

        when(rewardService.listOffers(100, 500, "price_asc")).thenReturn(List.of(
                new RewardService.RewardOfferView(
                        "1",
                        "Кофейня",
                        "Кофе",
                        "Описание",
                        100
                )
        ));

        when(rewardService.account("+79990000001")).thenReturn(
                new RewardService.AccountResp(
                        400,
                        900,
                        2,
                        "Проводник добра"
                )
        );

        when(rewardService.purchase(eq("+79990000001"), eq("1"), any(RewardService.PurchaseReq.class)))
                .thenReturn(new RewardService.PurchaseResp(
                        "50",
                        new RewardService.RewardOfferView(
                                "1",
                                "Кофейня",
                                "Кофе",
                                "Описание",
                                100
                        ),
                        300
                ));

        when(rewardService.leaderboard()).thenReturn(List.of(
                new RewardService.LeaderboardItem(
                        "10",
                        "Иван",
                        "Петров",
                        900,
                        "Проводник добра"
                )
        ));

        mvc.perform(get("/rewards")
                        .param("minPrice", "100")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partnerName").value("Кофейня"));

        mvc.perform(get("/rewards/account")
                        .principal(principal("+79990000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(400))
                .andExpect(jsonPath("$.title").value("Проводник добра"));

        mvc.perform(post("/rewards/1/purchase")
                        .principal(principal("+79990000001"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(300));

        mvc.perform(get("/rewards/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lifetimePoints").value(900));
    }

    private Principal principal(String phone) {
        return new UsernamePasswordAuthenticationToken(phone, "password");
    }
}