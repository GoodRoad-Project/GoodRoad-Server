package goodroad.rewards;

import goodroad.api.ApiErrors.ApiException;
import goodroad.points.PointLedgerService;
import goodroad.rewards.repository.*;
import goodroad.security.Crypto;
import goodroad.users.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class RewardService {
    private final UserRepo users;
    private final RewardOfferRepo offers;
    private final UserRewardPurchaseRepo purchases;
    private final PointLedgerService ledger;

    public RewardService(UserRepo users, RewardOfferRepo offers, UserRewardPurchaseRepo purchases, PointLedgerService ledger) {
        this.users = users;
        this.offers = offers;
        this.purchases = purchases;
        this.ledger = ledger;
    }

    public record RewardOfferView(String id, String partnerName, String title, String description, int price) {}
    public record PurchaseReq(boolean confirmed) {}
    public record PurchaseResp(String id, RewardOfferView reward, int balanceAfter) {}
    public record AccountResp(int balance, int lifetimePoints, int completedTasksCount, String title) {}
    public record LeaderboardItem(String userId, String firstName, String lastName, int lifetimePoints, String title) {}

    @Transactional(readOnly = true)
    public List<RewardOfferView> listOffers(Integer minPrice, Integer maxPrice, String sort) {
        boolean desc = "price_desc".equalsIgnoreCase(sort) || "desc".equalsIgnoreCase(sort);
        Comparator<RewardOfferEntity> comparator = Comparator.comparingInt(RewardOfferEntity::getPrice).thenComparing(RewardOfferEntity::getId);
        if (desc) comparator = comparator.reversed();
        return offers.findActiveFiltered(minPrice, maxPrice).stream().sorted(comparator).map(this::toView).toList();
    }

    @Transactional
    public PurchaseResp purchaseReward(String phoneFromAuth, String offerId, PurchaseReq req) {
        if (req == null || !req.confirmed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_PURCHASE_NOT_CONFIRMED", "Purchase must be confirmed");
        }
        UserEntity user = findCurrent(phoneFromAuth);
        RewardOfferEntity offer = offers.findById(parseId(offerId))
                .filter(RewardOfferEntity::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND", "Reward not found"));
        int price = offer.getPrice();
        ledger.spend(user, price, "REWARD_PURCHASE", offer.getPartnerName() + ": " + offer.getTitle(), offer.getId());
        UserRewardPurchaseEntity purchase = new UserRewardPurchaseEntity();
        purchase.setUserId(user.getId());
        purchase.setRewardOfferId(offer.getId());
        purchase.setPrice(price);
        purchases.save(purchase);
        return new PurchaseResp(purchase.getId() == null ? null : purchase.getId().toString(), toView(offer), Math.max(0, user.getTotalPoints()));
    }

    @Transactional(readOnly = true)
    public AccountResp getUserPointsInfo(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        int balance = safe(user.getTotalPoints());
        int lifetime = Math.max(safe(user.getLifetimePoints()), balance);
        return new AccountResp(balance, lifetime, safe(user.getCompletedTasksCount()), getTitleByPoints(lifetime));
    }

    @Transactional(readOnly = true)
    public List<PointLedgerService.PointTransactionView> getPurchaseHistory(String phoneFromAuth) {
        return ledger.history(findCurrent(phoneFromAuth));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardItem> getLeaderboard() {
        return users.findAll().stream()
                .sorted(Comparator.comparingInt((UserEntity u) -> -Math.max(safe(u.getLifetimePoints()), safe(u.getTotalPoints()))).thenComparing(UserEntity::getId))
                .map(user -> {
                    int lifetime = Math.max(safe(user.getLifetimePoints()), safe(user.getTotalPoints()));
                    return new LeaderboardItem(user.getId().toString(), user.getFirstName(), user.getLastName(), lifetime, getTitleByPoints(lifetime));
                }).toList();
    }

    public String getTitleByPoints(int points) {
        if (points >= 3000) return "Легенда добрых маршрутов";
        if (points >= 2500) return "Мастер доступного города";
        if (points >= 2000) return "Навигатор перемен";
        if (points >= 1500) return "Хранитель маршрутов";
        if (points >= 1000) return "Герой района";
        if (points >= 500) return "Проводник добра";
        if (points >= 100) return "Разведчик тротуаров";
        return "Новичок GoodRoad";
    }

    private RewardOfferView toView(RewardOfferEntity offer) {
        return new RewardOfferView(offer.getId() == null ? null : offer.getId().toString(), offer.getPartnerName(), offer.getTitle(), offer.getDescription(), offer.getPrice());
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        return users.findByPhoneHash(Crypto.sha256Hex(phoneNorm))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private Long parseId(String id) {
        try { return Long.parseLong(id); } catch (RuntimeException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Id is invalid"); }
    }
    private static int safe(Integer v) { return v == null ? 0 : Math.max(0, v); }
}
