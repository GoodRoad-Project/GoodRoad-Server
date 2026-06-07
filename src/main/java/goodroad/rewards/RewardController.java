package goodroad.rewards;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
public class RewardController {
    private final RewardService service;

    @GetMapping
    public List<RewardService.RewardOfferView> list(@RequestParam(required = false) Integer minPrice,
                                                    @RequestParam(required = false) Integer maxPrice,
                                                    @RequestParam(required = false, defaultValue = "price_asc") String sort) {
        return service.listOffers(minPrice, maxPrice, sort);
    }

    @PostMapping("/{id}/purchase")
    public RewardService.PurchaseResp purchase(Authentication authentication, @PathVariable String id, @RequestBody RewardService.PurchaseReq req) {
        return service.purchase(authentication.getName(), id, req);
    }

    @GetMapping("/account")
    public RewardService.AccountResp account(Authentication authentication) {
        return service.account(authentication.getName());
    }

    @GetMapping("/history")
    public List<goodroad.points.PointLedgerService.PointTransactionView> history(Authentication authentication) {
        return service.history(authentication.getName());
    }

    @GetMapping("/leaderboard")
    public List<RewardService.LeaderboardItem> leaderboard() {
        return service.leaderboard();
    }
}
