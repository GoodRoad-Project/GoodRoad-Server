package goodroad.points;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PointsControllerTest {

    private final PointLedgerService service = mock(PointLedgerService.class);
    private final MockMvc mvc = standaloneSetup(new PointsController(service)).build();

    @Test
    void shouldReturnCurrentUserPointsAccount() throws Exception {
        when(service.account("+79990000001")).thenReturn(new PointLedgerService.PointsAccountView(
                "10", 150, 250, 3, "Разведчик тротуаров"
        ));

        mvc.perform(get("/points/me")
                        .principal(new UsernamePasswordAuthenticationToken("+79990000001", "password"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("10"))
                .andExpect(jsonPath("$.balance").value(150))
                .andExpect(jsonPath("$.lifetimePoints").value(250))
                .andExpect(jsonPath("$.completedTasksCount").value(3))
                .andExpect(jsonPath("$.title").value("Разведчик тротуаров"));

        verify(service).account("+79990000001");
    }

    @Test
    void shouldReturnCurrentUserPointsHistory() throws Exception {
        when(service.historyForCurrentUser("+79990000001")).thenReturn(new PointLedgerService.PointsHistoryView(
                "10",
                120,
                List.of(
                        new PointLedgerService.PointTransactionView(
                                "2", "SPEND", 20, "REWARD_PURCHASE", "Скидка", null, "5", "REWARD_PURCHASE", "5",
                                Instant.parse("2026-06-07T18:30:00Z")
                        ),
                        new PointLedgerService.PointTransactionView(
                                "1", "EARN", 150, "REVIEW_APPROVED", "Отзыв одобрен", null, null, "REVIEW", "12",
                                Instant.parse("2026-06-07T17:30:00Z")
                        )
                )
        ));

        mvc.perform(get("/points/me/history")
                        .principal(new UsernamePasswordAuthenticationToken("+79990000001", "password"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("10"))
                .andExpect(jsonPath("$.balance").value(120))
                .andExpect(jsonPath("$.transactions[0].id").value("2"))
                .andExpect(jsonPath("$.transactions[0].direction").value("SPEND"))
                .andExpect(jsonPath("$.transactions[0].amount").value(20))
                .andExpect(jsonPath("$.transactions[0].rewardOfferId").value("5"))
                .andExpect(jsonPath("$.transactions[1].id").value("1"))
                .andExpect(jsonPath("$.transactions[1].direction").value("EARN"))
                .andExpect(jsonPath("$.transactions[1].reason").value("REVIEW_APPROVED"));

        verify(service).historyForCurrentUser("+79990000001");
    }

    @Test
    void shouldReturnPointsLeaderboard() throws Exception {
        when(service.leaderboard()).thenReturn(List.of(
                new PointLedgerService.LeaderboardItem("20", "Анна", "Иванова", 900, "Проводник добра"),
                new PointLedgerService.LeaderboardItem("10", "Иван", "Петров", 150, "Разведчик тротуаров")
        ));

        mvc.perform(get("/points/leaderboard").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("20"))
                .andExpect(jsonPath("$[0].firstName").value("Анна"))
                .andExpect(jsonPath("$[0].lifetimePoints").value(900))
                .andExpect(jsonPath("$[0].title").value("Проводник добра"))
                .andExpect(jsonPath("$[1].userId").value("10"));

        verify(service).leaderboard();
    }
}
