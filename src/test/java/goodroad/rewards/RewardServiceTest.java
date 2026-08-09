package goodroad.rewards;

import goodroad.api.ApiErrors.ApiException;
import goodroad.points.PointLedgerService;
import goodroad.rewards.repository.RewardInventoryItemEntity;
import goodroad.rewards.repository.RewardInventoryItemRepo;
import goodroad.rewards.repository.RewardOfferEntity;
import goodroad.rewards.repository.RewardOfferRepo;
import goodroad.rewards.repository.UserRewardPurchaseEntity;
import goodroad.rewards.repository.UserRewardPurchaseRepo;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {
    @Mock UserRepo users;
    @Mock RewardOfferRepo offers;
    @Mock RewardInventoryItemRepo inventory;
    @Mock UserRewardPurchaseRepo purchases;
    @Mock PointLedgerService ledger;
    @InjectMocks RewardService service;

    @Test
    void listsOnlyOffersThatHaveConcreteInventory() {
        RewardOfferEntity available = offer(1L, "Кофейня", "Кофе", 100);
        RewardOfferEntity empty = offer(2L, "Кино", "Билет", 300);
        when(offers.findActiveFiltered(100, 300)).thenReturn(List.of(available, empty));
        when(inventory.countAvailable(eq(1L), any())).thenReturn(3L);
        when(inventory.countAvailable(eq(2L), any())).thenReturn(0L);

        List<RewardService.RewardOfferView> result = service.listOffers(100, 300, "price_desc");

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals(3, result.get(0).availableQuantity());
    }

    @Test
    void rejectsInvalidOfferFilters() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.listOffers(500, 100, "price_asc")
        );

        assertEquals("REWARD_PRICE_FILTER_INVALID", exception.code());
        verify(offers, never()).findActiveFiltered(any(), any());
    }

    @Test
    void purchaseAtomicallyAssignsOneCodeAndSpendsPoints() {
        UserEntity user = user(10L, 500);
        RewardOfferEntity offer = offer(1L, "Буквоед", "Скидка", 250);
        RewardInventoryItemEntity item = item(50L, 1L, "BOOK-25");
        when(users.findByPhoneHashForUpdate(any())).thenReturn(Optional.of(user));
        when(offers.findById(1L)).thenReturn(Optional.of(offer));
        when(inventory.findAvailableForUpdate(eq(1L), any(), any())).thenReturn(List.of(item));
        when(inventory.countAvailable(eq(1L), any())).thenReturn(0L);
        doAnswer(invocation -> {
            UserEntity current = invocation.getArgument(0);
            current.setTotalPoints(current.getTotalPoints() - (Integer) invocation.getArgument(1));
            return null;
        }).when(ledger).spend(eq(user), eq(250), eq("REWARD_PURCHASE"), eq("Буквоед: Скидка"), eq(1L));
        when(purchases.save(any(UserRewardPurchaseEntity.class))).thenAnswer(invocation -> {
            UserRewardPurchaseEntity purchase = invocation.getArgument(0);
            purchase.setId(100L);
            purchase.setCreatedAt(Instant.now());
            return purchase;
        });

        RewardService.PurchaseResp result = service.purchaseReward(
                "+79990000001", "1", new RewardService.PurchaseReq(true)
        );

        assertEquals("100", result.id());
        assertEquals("BOOK-25", result.issuedReward().code());
        assertEquals(250, result.balanceAfter());
        assertTrue(result.issuedReward().expiresAt().isAfter(Instant.now().plusSeconds(29L * 24 * 3600)));
        assertEquals("ASSIGNED", item.getStatus());
        verify(inventory).save(item);
        verify(ledger).spend(user, 250, "REWARD_PURCHASE", "Буквоед: Скидка", 1L);
    }

    @Test
    void outOfStockDoesNotSpendPoints() {
        UserEntity user = user(10L, 500);
        when(users.findByPhoneHashForUpdate(any())).thenReturn(Optional.of(user));
        when(offers.findById(1L)).thenReturn(Optional.of(offer(1L, "Партнер", "Купон", 100)));
        when(inventory.findAvailableForUpdate(eq(1L), any(), any())).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () -> service.purchaseReward(
                "+79990000001", "1", new RewardService.PurchaseReq(true)
        ));

        assertEquals("REWARD_OUT_OF_STOCK", exception.code());
        verify(ledger, never()).spend(any(), anyInt(), any(), any(), any());
    }

    @Test
    void separatesActiveAndExpiredUserRewards() {
        UserEntity user = user(10L, 500);
        UserRewardPurchaseEntity active = purchase(100L, 10L, 1L, 50L, "ACTIVE", Instant.now().plusSeconds(3600));
        UserRewardPurchaseEntity expired = purchase(101L, 10L, 1L, 51L, "ACTIVE", Instant.now().minusSeconds(1));
        when(users.findByPhoneHash(any())).thenReturn(Optional.of(user));
        when(purchases.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(active, expired));
        when(offers.findById(1L)).thenReturn(Optional.of(offer(1L, "Партнер", "Купон", 100)));
        when(inventory.findById(50L)).thenReturn(Optional.of(item(50L, 1L, "ACTIVE-CODE")));
        when(inventory.findById(51L)).thenReturn(Optional.of(item(51L, 1L, "EXPIRED-CODE")));

        RewardService.UserRewardsResp result = service.listCurrentUserRewards("+79990000001");

        assertEquals(1, result.active().size());
        assertEquals(1, result.inactive().size());
        assertEquals("EXPIRED", result.inactive().get(0).status());
        assertTrue(result.inactive().get(0).expiresAt().isBefore(Instant.now()));
        verify(purchases).save(expired);
    }

    @Test
    void marksExpiredRewardWhenRedeemIsAttempted() {
        UserEntity user = user(10L, 500);
        UserRewardPurchaseEntity expired = purchase(
                101L, 10L, 1L, 51L, "ACTIVE", Instant.now().minusSeconds(1)
        );
        when(users.findByPhoneHashForUpdate(any())).thenReturn(Optional.of(user));
        when(purchases.findByIdAndUserIdForUpdate(101L, 10L)).thenReturn(Optional.of(expired));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.redeem("+79990000001", "101")
        );

        assertEquals("USER_REWARD_EXPIRED", exception.code());
        assertEquals("EXPIRED", expired.getStatus());
        verify(purchases).save(expired);
    }

    private RewardOfferEntity offer(Long id, String partner, String title, int price) {
        RewardOfferEntity offer = new RewardOfferEntity();
        offer.setId(id);
        offer.setPartnerName(partner);
        offer.setTitle(title);
        offer.setDescription("Описание");
        offer.setRewardType("PROMOCODE");
        offer.setValidityDays(30);
        offer.setPrice(price);
        offer.setActive(true);
        return offer;
    }

    private RewardInventoryItemEntity item(Long id, Long offerId, String code) {
        RewardInventoryItemEntity item = new RewardInventoryItemEntity();
        item.setId(id);
        item.setRewardOfferId(offerId);
        item.setCode(code);
        item.setStatus("AVAILABLE");
        return item;
    }

    private UserRewardPurchaseEntity purchase(
            Long id, Long userId, Long offerId, Long itemId, String status, Instant expiresAt
    ) {
        UserRewardPurchaseEntity purchase = new UserRewardPurchaseEntity();
        purchase.setId(id);
        purchase.setUserId(userId);
        purchase.setRewardOfferId(offerId);
        purchase.setInventoryItemId(itemId);
        purchase.setPrice(100);
        purchase.setStatus(status);
        purchase.setCreatedAt(Instant.now());
        purchase.setExpiresAt(expiresAt);
        return purchase;
    }

    private UserEntity user(Long id, int balance) {
        UserEntity user = UserEntity.builder().role("USER").active(true).totalPoints(balance).build();
        user.setId(id);
        return user;
    }
}
