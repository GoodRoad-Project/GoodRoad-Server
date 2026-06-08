package goodroad.rewards;

import goodroad.api.ApiErrors.ApiException;
import goodroad.points.PointLedgerService;
import goodroad.rewards.repository.*;
import goodroad.security.Crypto;
import goodroad.users.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {
    @Mock UserRepo users;
    @Mock RewardOfferRepo offers;
    @Mock UserRewardPurchaseRepo purchases;
    @Mock PointLedgerService ledger;
    @InjectMocks RewardService service;

    @Test
    void shouldFilterAndSortRewardsByPriceDesc() {
        RewardOfferEntity cheap = offer(1L, "Кофейня", "Кофе", 100);
        RewardOfferEntity expensive = offer(2L, "Кино", "Билет", 300);
        when(offers.findActiveFiltered(100, 300)).thenReturn(List.of(cheap, expensive));

        List<RewardService.RewardOfferView> result = service.listOffers(100, 300, "price_desc");

        assertEquals("2", result.get(0).id());
        assertEquals(300, result.get(0).price());
        assertEquals("1", result.get(1).id());
    }

    @Test
    void shouldRequirePurchaseConfirmation() {
        ApiException ex = assertThrows(ApiException.class, () -> service.purchaseReward("+79990000001", "1", new RewardService.PurchaseReq(false)));
        assertEquals("REWARD_PURCHASE_NOT_CONFIRMED", ex.code());
    }

    @Test
    void shouldPurchaseRewardAndWriteSpendTransaction() {
        UserEntity user = user(10L, 500, 700);
        RewardOfferEntity offer = offer(1L, "Буквоед", "Скидка", 250);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
        when(offers.findById(1L)).thenReturn(Optional.of(offer));
        doAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setTotalPoints(u.getTotalPoints() - (Integer) inv.getArgument(1));
            return null;
        }).when(ledger).spend(eq(user), eq(250), eq("REWARD_PURCHASE"), eq("Буквоед: Скидка"), eq(1L));
        when(purchases.save(any(UserRewardPurchaseEntity.class))).thenAnswer(inv -> {
            UserRewardPurchaseEntity purchase = inv.getArgument(0);
            purchase.setId(100L);
            return purchase;
        });

        RewardService.PurchaseResp result = service.purchaseReward("+79990000001", "1", new RewardService.PurchaseReq(true));

        assertEquals("100", result.id());
        verify(ledger).spend(user, 250, "REWARD_PURCHASE", "Буквоед: Скидка", 1L);
    }

    @Test
    void shouldBuildAccountTitleFromLifetimePoints() {
        UserEntity user = user(10L, 10, 1200);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));

        RewardService.AccountResp account = service.getUserPointsInfo("+79990000001");

        assertEquals(10, account.balance());
        assertEquals(1200, account.lifetimePoints());
        assertEquals("Герой района", account.title());
    }

    private RewardOfferEntity offer(Long id, String partner, String title, int price) {
        RewardOfferEntity offer = new RewardOfferEntity();
        offer.setId(id);
        offer.setPartnerName(partner);
        offer.setTitle(title);
        offer.setDescription("Описание");
        offer.setPrice(price);
        offer.setActive(true);
        return offer;
    }

    private UserEntity user(Long id, int balance, int lifetime) {
        UserEntity user = UserEntity.builder()
                .firstName("Иван")
                .lastName("Петров")
                .phoneHash(Crypto.sha256Hex("79990000001"))
                .role("USER")
                .active(true)
                .totalPoints(balance)
                .lifetimePoints(lifetime)
                .completedTasksCount(0)
                .build();
        user.setId(id);
        return user;
    }
}
