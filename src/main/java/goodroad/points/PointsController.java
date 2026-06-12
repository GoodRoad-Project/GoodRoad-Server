package goodroad.points;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {
    private final PointLedgerService service;

    @GetMapping("/me")
    public PointLedgerService.PointsAccountView account(Authentication authentication) {
        return service.account(authentication.getName());
    }

    @GetMapping("/me/history")
    public PointLedgerService.PointsHistoryView history(Authentication authentication) {
        return service.historyForCurrentUser(authentication.getName());
    }

    @GetMapping("/leaderboard")
    public List<PointLedgerService.LeaderboardItem> leaderboard() {
        return service.leaderboard();
    }
}
