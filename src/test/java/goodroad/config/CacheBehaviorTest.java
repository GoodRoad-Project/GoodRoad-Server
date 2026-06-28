package goodroad.config;

import goodroad.model.gh.GraphHopperResponse;
import goodroad.points.PointLedgerService;
import goodroad.points.repository.PointTransactionRepo;
import goodroad.rewards.RewardService;
import goodroad.rewards.repository.RewardOfferEntity;
import goodroad.rewards.repository.RewardOfferRepo;
import goodroad.rewards.repository.UserRewardPurchaseRepo;
import goodroad.service.GraphHopperService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheBehaviorTest {

    @Test
    void shouldCacheGraphHopperRouteByRouteParametersAndCustomModelHash() throws IOException {
        try (MockWebServer mockWebServer = new MockWebServer()) {
            mockWebServer.start();
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                        {
                          "paths": [
                            {
                              "distance": 1250.0,
                              "time": 600000,
                              "points": "encoded_points_string",
                              "points_encoded": true
                            }
                          ]
                        }
                        """));

            String rawBaseUrl = mockWebServer.url("").toString();
            final String baseUrl = rawBaseUrl.substring(0, rawBaseUrl.length() - 1);

            try (AnnotationConfigApplicationContext context = cacheContext()) {
                context.registerBean(GraphHopperService.class, () -> new GraphHopperService("test-api-key", baseUrl));
                context.refresh();

                GraphHopperService service = context.getBean(GraphHopperService.class);
                Map<String, Object> customModel = Map.of("priority", List.of(Map.of("if", "road_class == STEPS", "multiply_by", 0.1)));

                GraphHopperResponse first = service.getRoute(
                        "59.932480,30.262920",
                        "59.928767,30.264197",
                        "foot",
                        true,
                        "ru",
                        customModel
                );
                GraphHopperResponse second = service.getRoute(
                        "59.932480,30.262920",
                        "59.928767,30.264197",
                        "foot",
                        true,
                        "ru",
                        customModel
                );

                assertNotNull(first);
                assertNotNull(second);
                assertEquals(1250.0, second.getPaths().get(0).getDistance(), 0.01);
                assertEquals(1, mockWebServer.getRequestCount());
            }
        }
    }

    @Test
    void shouldUseSameGraphHopperCacheKeyForEquivalentCustomModels() throws NoSuchMethodException {
        KeyGenerator generator = new CacheConfig().graphHopperRouteKeyGenerator();
        var method = GraphHopperService.class.getMethod(
                "getRoute",
                String.class,
                String.class,
                String.class,
                Boolean.class,
                String.class,
                Map.class
        );

        Map<String, Object> firstModel = new java.util.LinkedHashMap<>();
        firstModel.put("priority", List.of(Map.of("if", "road_class == STEPS", "multiply_by", 0.1)));
        firstModel.put("speed", List.of(Map.of("if", "surface == GRAVEL", "multiply_by", 0.7)));

        Map<String, Object> secondModel = new java.util.LinkedHashMap<>();
        secondModel.put("speed", List.of(Map.of("if", "surface == GRAVEL", "multiply_by", 0.7)));
        secondModel.put("priority", List.of(Map.of("if", "road_class == STEPS", "multiply_by", 0.1)));

        Object firstKey = generator.generate(
                new GraphHopperService("test-api-key", "http://localhost"),
                method,
                "59.932480,30.262920",
                "59.928767,30.264197",
                "FOOT",
                true,
                "RU",
                firstModel
        );
        Object secondKey = generator.generate(
                new GraphHopperService("test-api-key", "http://localhost"),
                method,
                "59.932480,30.262920",
                "59.928767,30.264197",
                "foot",
                true,
                "ru",
                secondModel
        );

        assertEquals(firstKey, secondKey);
    }

    @Test
    void shouldCacheRewardOffersForSameFilterAndSort() {
        UserRepo users = mock(UserRepo.class);
        RewardOfferRepo offers = mock(RewardOfferRepo.class);
        UserRewardPurchaseRepo purchases = mock(UserRewardPurchaseRepo.class);
        PointLedgerService ledger = mock(PointLedgerService.class);

        when(offers.findActiveFiltered(100, 300)).thenReturn(List.of(
                offer(1L, "Кофейня", "Кофе", 100),
                offer(2L, "Кино", "Билет", 300)
        ));

        try (AnnotationConfigApplicationContext context = cacheContext()) {
            context.registerBean(RewardService.class, () -> new RewardService(users, offers, purchases, ledger));
            context.refresh();

            RewardService service = context.getBean(RewardService.class);

            List<RewardService.RewardOfferView> first = service.listOffers(100, 300, "price_desc");
            List<RewardService.RewardOfferView> second = service.listOffers(100, 300, "price_desc");

            assertEquals(2, first.size());
            assertEquals(2, second.size());
            assertEquals("2", second.get(0).id());
            verify(offers, times(1)).findActiveFiltered(100, 300);
        }
    }

    @Test
    void shouldEvictLeaderboardsWhenPointsAreEarned() {
        UserRepo users = mock(UserRepo.class);
        PointTransactionRepo transactions = mock(PointTransactionRepo.class);
        UserEntity user = user(10L, 100, 200);

        try (AnnotationConfigApplicationContext context = cacheContext()) {
            context.registerBean(PointLedgerService.class, () -> new PointLedgerService(users, transactions));
            context.refresh();

            CacheManager cacheManager = context.getBean(CacheManager.class);
            Cache pointsLeaderboard = requireCache(cacheManager, CacheConfig.POINTS_LEADERBOARD);
            Cache rewardLeaderboard = requireCache(cacheManager, CacheConfig.REWARD_LEADERBOARD);
            pointsLeaderboard.put("all", List.of("stale points leaderboard"));
            rewardLeaderboard.put("all", List.of("stale reward leaderboard"));

            PointLedgerService service = context.getBean(PointLedgerService.class);
            service.earn(user, 50, "TEST", "test earn", null, "TEST", null);

            assertNull(pointsLeaderboard.get("all"));
            assertNull(rewardLeaderboard.get("all"));
            assertEquals(150, user.getTotalPoints());
            assertEquals(250, user.getLifetimePoints());
            verify(users).save(user);
            verify(transactions).save(any());
        }
    }

    private static AnnotationConfigApplicationContext cacheContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestCachingConfig.class);
        context.registerBean(CacheManager.class, () -> new ConcurrentMapCacheManager(
                CacheConfig.GRAPH_HOPPER_ROUTES,
                CacheConfig.REWARD_OFFERS,
                CacheConfig.REWARD_LEADERBOARD,
                CacheConfig.POINTS_LEADERBOARD
        ));
        context.registerBean("graphHopperRouteKeyGenerator", KeyGenerator.class, () -> new CacheConfig().graphHopperRouteKeyGenerator());
        return context;
    }

    private static Cache requireCache(CacheManager cacheManager, String name) {
        Cache cache = cacheManager.getCache(name);
        assertNotNull(cache, "Cache not found: " + name);
        return cache;
    }

    private static RewardOfferEntity offer(Long id, String partner, String title, int price) {
        RewardOfferEntity offer = new RewardOfferEntity();
        offer.setId(id);
        offer.setPartnerName(partner);
        offer.setTitle(title);
        offer.setDescription("Описание");
        offer.setPrice(price);
        offer.setActive(true);
        return offer;
    }

    private static UserEntity user(Long id, int balance, int lifetime) {
        UserEntity user = UserEntity.builder()
                .firstName("Иван")
                .lastName("Петров")
                .role("USER")
                .active(true)
                .totalPoints(balance)
                .lifetimePoints(lifetime)
                .completedTasksCount(0)
                .build();
        user.setId(id);
        return user;
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestCachingConfig {
    }
}
