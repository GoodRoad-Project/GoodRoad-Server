package goodroad.rewards;

import goodroad.api.ApiErrors.ApiException;
import goodroad.config.CacheConfig;
import goodroad.points.PointLedgerService;
import goodroad.rewards.repository.*;
import goodroad.security.Crypto;
import goodroad.users.repository.*;
import goodroad.validation.InputRules;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class RewardService {
    private final UserRepo users;
    private final RewardOfferRepo offers;
    private final RewardInventoryItemRepo inventory;
    private final UserRewardPurchaseRepo purchases;
    private final PointLedgerService ledger;

    public RewardService(UserRepo users, RewardOfferRepo offers,
                         RewardInventoryItemRepo inventory, UserRewardPurchaseRepo purchases, PointLedgerService ledger) {
        this.users = users;
        this.offers = offers;
        this.inventory = inventory;
        this.purchases = purchases;
        this.ledger = ledger;
    }

    public record RewardOfferView(String id, String partnerName, String title, String description, String rewardType, int price, long availableQuantity) {}
    public record PurchaseReq(boolean confirmed) {}
    public record PurchaseResp(String id, RewardOfferView reward, UserRewardView issuedReward, int balanceAfter) {}
    public record UserRewardView(String id, String offerId, String partnerName, String title, String rewardType,
                                 String code, int pricePaid, String status, java.time.Instant purchasedAt,
                                 java.time.Instant expiresAt, java.time.Instant redeemedAt) {}
    public record UserRewardsResp(List<UserRewardView> active, List<UserRewardView> inactive) {}
    public record InventoryItemReq(String code, java.time.Instant validUntil) {}
    public record InventoryCreatedResp(int created) {}

    @Cacheable(cacheNames = CacheConfig.REWARD_OFFERS, key = "T(java.util.Objects).toString(#minPrice, '') + '|' + T(java.util.Objects).toString(#maxPrice, '') + '|' + (#sort == null ? 'price_asc' : #sort.toLowerCase())", unless = "#result == null")
    @Transactional(readOnly = true)
    public List<RewardOfferView> listOffers(Integer minPrice, Integer maxPrice, String sort) {
        if ((minPrice != null && minPrice < 0)
                || (maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_PRICE_FILTER_INVALID", "Укажите корректный диапазон стоимости наград");
        }
        String normalizedSort = sort == null ? "price_asc" : sort.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("price_asc", "price_desc", "asc", "desc").contains(normalizedSort)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_SORT_INVALID", "Сортировка должна быть price_asc или price_desc");
        }
        boolean desc = "price_desc".equals(normalizedSort) || "desc".equals(normalizedSort);
        Comparator<RewardOfferEntity> comparator = Comparator.comparingInt(RewardOfferEntity::getPrice).thenComparing(RewardOfferEntity::getId);
        if (desc) comparator = comparator.reversed();
        return offers.findActiveFiltered(minPrice, maxPrice).stream()
                .sorted(comparator)
                .map(this::toView)
                .filter(view -> view.availableQuantity() > 0)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.REWARD_OFFERS, allEntries = true)
    public PurchaseResp purchaseReward(String phoneFromAuth, String offerId, PurchaseReq req) {
        if (req == null || !req.confirmed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_PURCHASE_NOT_CONFIRMED", "Подтвердите покупку награды");
        }
        UserEntity user = findCurrent(phoneFromAuth, true);
        RewardOfferEntity offer = offers.findById(parseId(offerId, "REWARD_ID_INVALID"))
                .filter(RewardOfferEntity::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND", "Награда не найдена"));
        java.time.Instant now = java.time.Instant.now();
        RewardInventoryItemEntity item = inventory.findAvailableForUpdate(offer.getId(), now, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "REWARD_OUT_OF_STOCK", "Эта награда закончилась"));
        int price = offer.getPrice();
        ledger.spend(user, price, "REWARD_PURCHASE", offer.getPartnerName() + ": " + offer.getTitle(), offer.getId());
        item.setStatus("ASSIGNED");
        item.setAssignedAt(now);
        inventory.save(item);

        UserRewardPurchaseEntity purchase = new UserRewardPurchaseEntity();
        purchase.setUserId(user.getId());
        purchase.setRewardOfferId(offer.getId());
        purchase.setInventoryItemId(item.getId());
        purchase.setPrice(price);
        purchase.setStatus("ACTIVE");
        purchase.setExpiresAt(calculateExpiry(offer, item, now));
        purchases.save(purchase);
        return new PurchaseResp(
                purchase.getId() == null ? null : purchase.getId().toString(),
                toView(offer),
                toUserReward(purchase, offer, item),
                safe(user.getTotalPoints())
        );
    }

    @Transactional
    public UserRewardsResp listCurrentUserRewards(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth, false);
        java.time.Instant now = java.time.Instant.now();
        List<UserRewardView> active = new ArrayList<>();
        List<UserRewardView> inactive = new ArrayList<>();
        for (UserRewardPurchaseEntity purchase : purchases.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            if ("ACTIVE".equals(purchase.getStatus()) && purchase.getExpiresAt() != null && !purchase.getExpiresAt().isAfter(now)) {
                purchase.setStatus("EXPIRED");
                purchases.save(purchase);
            }
            RewardOfferEntity offer = offers.findById(purchase.getRewardOfferId()).orElse(null);
            RewardInventoryItemEntity item = inventory.findById(purchase.getInventoryItemId()).orElse(null);
            if (offer == null || item == null) {
                continue;
            }
            UserRewardView view = toUserReward(purchase, offer, item);
            if ("ACTIVE".equals(view.status())) {
                active.add(view);
            } else {
                inactive.add(view);
            }
        }
        return new UserRewardsResp(active, inactive);
    }

    @Transactional(noRollbackFor = ExpiredRewardException.class)
    public UserRewardView redeem(String phoneFromAuth, String purchaseId) {
        UserEntity user = findCurrent(phoneFromAuth, true);
        long parsedId = parseId(purchaseId, "USER_REWARD_ID_INVALID");
        UserRewardPurchaseEntity purchase = purchases.findByIdAndUserIdForUpdate(parsedId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_REWARD_NOT_FOUND", "Купленная награда не найдена"));
        java.time.Instant now = java.time.Instant.now();
        if (purchase.getExpiresAt() != null && !purchase.getExpiresAt().isAfter(now)) {
            purchase.setStatus("EXPIRED");
            purchases.save(purchase);
            throw new ExpiredRewardException();
        }
        if (!"ACTIVE".equals(purchase.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_REWARD_NOT_ACTIVE", "Награда уже неактивна");
        }
        purchase.setStatus("REDEEMED");
        purchase.setRedeemedAt(now);
        purchases.save(purchase);
        RewardOfferEntity offer = offers.findById(purchase.getRewardOfferId()).orElseThrow();
        RewardInventoryItemEntity item = inventory.findById(purchase.getInventoryItemId()).orElseThrow();
        return toUserReward(purchase, offer, item);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.REWARD_OFFERS, allEntries = true)
    public InventoryCreatedResp addInventory(String offerId, List<InventoryItemReq> requests) {
        long parsedOfferId = parseId(offerId, "REWARD_ID_INVALID");
        RewardOfferEntity offer = offers.findById(parsedOfferId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND", "Награда не найдена"));
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_INVENTORY_EMPTY", "Добавьте хотя бы один код награды");
        }
        int created = 0;
        for (InventoryItemReq request : requests) {
            String code = InputRules.trimToNull(request == null ? null : request.code());
            if (code == null || code.length() > 512 || code.chars().anyMatch(Character::isISOControl)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_CODE_INVALID", "Код награды не может быть пустым или длиннее 512 символов");
            }
            if (request.validUntil() != null && !request.validUntil().isAfter(java.time.Instant.now())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "REWARD_VALID_UNTIL_INVALID", "Срок действия кода должен быть в будущем");
            }
            RewardInventoryItemEntity item = new RewardInventoryItemEntity();
            item.setRewardOfferId(offer.getId());
            item.setCode(code);
            item.setValidUntil(request.validUntil());
            inventory.save(item);
            created++;
        }
        return new InventoryCreatedResp(created);
    }

    private RewardOfferView toView(RewardOfferEntity offer) {
        return new RewardOfferView(
                offer.getId() == null ? null : offer.getId().toString(),
                offer.getPartnerName(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getRewardType(),
                offer.getPrice(),
                offer.getId() == null ? 0 : inventory.countAvailable(offer.getId(), java.time.Instant.now())
        );
    }

    private UserRewardView toUserReward(UserRewardPurchaseEntity purchase, RewardOfferEntity offer, RewardInventoryItemEntity item) {
        return new UserRewardView(
                purchase.getId() == null ? null : purchase.getId().toString(),
                offer.getId().toString(),
                offer.getPartnerName(),
                offer.getTitle(),
                offer.getRewardType(),
                item.getCode(),
                purchase.getPrice(),
                purchase.getStatus(),
                purchase.getCreatedAt(),
                purchase.getExpiresAt(),
                purchase.getRedeemedAt()
        );
    }

    private java.time.Instant calculateExpiry(RewardOfferEntity offer, RewardInventoryItemEntity item, java.time.Instant purchasedAt) {
        java.time.Instant offerExpiry = offer.getValidityDays() == null ? null : purchasedAt.plus(java.time.Duration.ofDays(offer.getValidityDays()));
        if (offerExpiry == null) return item.getValidUntil();
        if (item.getValidUntil() == null) return offerExpiry;
        return offerExpiry.isBefore(item.getValidUntil()) ? offerExpiry : item.getValidUntil();
    }

    private UserEntity findCurrent(String phoneFromAuth, boolean forUpdate) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        }
        String phoneHash = Crypto.sha256Hex(phoneNorm);
        Optional<UserEntity> result = forUpdate
                ? users.findByPhoneHashForUpdate(phoneHash)
                : users.findByPhoneHash(phoneHash);
        return result.filter(UserEntity::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private long parseId(String raw, String code) {
        try {
            long value = Long.parseLong(raw);
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "Id is invalid");
        }
    }

    private static int safe(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static final class ExpiredRewardException extends ApiException {
        private ExpiredRewardException() {
            super(HttpStatus.CONFLICT, "USER_REWARD_EXPIRED", "Срок действия награды истек");
        }
    }
}
