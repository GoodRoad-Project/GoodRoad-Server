package goodroad.rewards;

import lombok.RequiredArgsConstructor;
import goodroad.points.PointLedgerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
public class RewardController {
    private final RewardService service;
    private final PointLedgerService points;

    @GetMapping
    public List<RewardService.RewardOfferView> list(
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false, defaultValue = "price_asc") String sort) {

        return service.listOffers(minPrice, maxPrice, sort);
    }

    @PostMapping("/{id}/purchase")
    public RewardService.PurchaseResp purchaseReward(Authentication authentication, @PathVariable String id, @RequestBody RewardService.PurchaseReq req) {
        return service.purchaseReward(authentication.getName(), id, req);
    }

    @GetMapping("/account")
    public PointLedgerService.PointsAccountView getUserPointsInfo(Authentication authentication) {
        return points.account(authentication.getName());
    }

    @GetMapping("/history")
    public List<goodroad.points.PointLedgerService.PointTransactionView> history(Authentication authentication) {
        return points.historyForCurrentUser(authentication.getName()).transactions();
    }

    @GetMapping("/leaderboard")
    public List<PointLedgerService.LeaderboardItem> getLeaderboard() {
        return points.leaderboard();
    }

    @GetMapping("/me")
    public RewardService.UserRewardsResp currentUserRewards(Authentication authentication) {
        return service.listCurrentUserRewards(authentication.getName());
    }

    @PostMapping("/me/{purchaseId}/redeem")
    public RewardService.UserRewardView redeem(Authentication authentication, @PathVariable String purchaseId) {
        return service.redeem(authentication.getName(), purchaseId);
    }

    @PostMapping("/admin/offers/{offerId}/inventory")
    @PreAuthorize("hasRole('MODERATOR_ADMIN')")
    public RewardService.InventoryCreatedResp addInventory(
            @PathVariable String offerId,
            @RequestBody List<RewardService.InventoryItemReq> requests
    ) {
        return service.addInventory(offerId, requests);
    }
}
