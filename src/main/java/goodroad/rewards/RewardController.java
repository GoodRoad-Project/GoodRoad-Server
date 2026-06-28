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
    public List<RewardService.RewardOfferView> list(
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false, defaultValue = "price_asc") String sort) {

        long start = System.nanoTime();

        try {
            return service.listOffers(minPrice, maxPrice, sort);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("GET /rewards took " + ms + " ms");
        }
    }

    @PostMapping("/{id}/purchase")
    public RewardService.PurchaseResp purchaseReward(Authentication authentication, @PathVariable String id, @RequestBody RewardService.PurchaseReq req) {
        return service.purchaseReward(authentication.getName(), id, req);
    }

    @GetMapping("/account")
    public RewardService.AccountResp getUserPointsInfo(Authentication authentication) {
        return service.getUserPointsInfo(authentication.getName());
    }

    @GetMapping("/history")
    public List<goodroad.points.PointLedgerService.PointTransactionView> history(Authentication authentication) {
        return service.getPurchaseHistory(authentication.getName());
    }

    @GetMapping("/leaderboard")
    public List<RewardService.LeaderboardItem> getLeaderboard() {
        return service.getLeaderboard();
    }
}
