package goodroad.points;

import goodroad.api.ApiErrors.ApiException;
import goodroad.config.CacheConfig;
import goodroad.points.repository.*;
import goodroad.security.Crypto;
import goodroad.users.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PointLedgerService {
    private final UserRepo users;
    private final PointTransactionRepo transactions;

    public PointLedgerService(UserRepo users, PointTransactionRepo transactions) {
        this.users = users;
        this.transactions = transactions;
    }

    public record PointsAccountView(String userId, int balance, int lifetimePoints, int completedTasksCount, String title) {}
    public record PointsHistoryView(String userId, int balance, List<PointTransactionView> transactions) {}
    public record LeaderboardItem(String userId, String firstName, String lastName, int lifetimePoints, String title) {}
    public record PointTransactionView(String id, String direction, int amount, String reason, String details, String taskId, String rewardOfferId, String sourceType, String sourceId, java.time.Instant createdAt) {}

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.POINTS_LEADERBOARD, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.REWARD_LEADERBOARD, allEntries = true)
    })
    @Transactional
    public void earn(UserEntity user, int amount, String reason, String details, Long taskId, String sourceType, Long sourceId) {
        if (amount <= 0) return;
        user.setTotalPoints(safe(user.getTotalPoints()) + amount);
        user.setLifetimePoints(safe(user.getLifetimePoints()) + amount);
        users.save(user);
        saveTx(user.getId(), "EARN", amount, reason, details, taskId, null, sourceType, sourceId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.POINTS_LEADERBOARD, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.REWARD_LEADERBOARD, allEntries = true)
    })
    @Transactional
    public void spend(UserEntity user, int amount, String reason, String details, Long rewardOfferId) {
        if (amount <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "POINT_AMOUNT_INVALID", "Point amount is invalid");
        if (safe(user.getTotalPoints()) < amount) {
            throw new ApiException(HttpStatus.CONFLICT, "POINT_BALANCE_NOT_ENOUGH", "Not enough points");
        }
        user.setTotalPoints(safe(user.getTotalPoints()) - amount);
        users.save(user);
        saveTx(user.getId(), "SPEND", amount, reason, details, null, rewardOfferId, "REWARD_PURCHASE", rewardOfferId);
    }

    @Transactional(readOnly = true)
    public PointsAccountView account(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        return toAccountView(user);
    }

    @Transactional(readOnly = true)
    public PointsHistoryView historyForCurrentUser(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        return new PointsHistoryView(
                user.getId().toString(),
                safe(user.getTotalPoints()),
                history(user)
        );
    }

    @Transactional(readOnly = true)
    public List<PointTransactionView> history(UserEntity user) {
        return transactions.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(PointLedgerService::toView).toList();
    }

    @Cacheable(cacheNames = CacheConfig.POINTS_LEADERBOARD, key = "'all'", unless = "#result == null")
    @Transactional(readOnly = true)
    public List<LeaderboardItem> leaderboard() {
        return users.findAll().stream()
                .sorted(Comparator.comparingInt((UserEntity u) -> -Math.max(safe(u.getLifetimePoints()), safe(u.getTotalPoints()))).thenComparing(UserEntity::getId))
                .map(user -> {
                    int lifetime = Math.max(safe(user.getLifetimePoints()), safe(user.getTotalPoints()));
                    return new LeaderboardItem(user.getId().toString(), user.getFirstName(), user.getLastName(), lifetime, titleFor(lifetime));
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void saveTx(Long userId, String direction, int amount, String reason, String details, Long taskId, Long rewardOfferId, String sourceType, Long sourceId) {
        PointTransactionEntity tx = new PointTransactionEntity();
        tx.setUserId(userId);
        tx.setDirection(direction);
        tx.setAmount(amount);
        tx.setReason(reason);
        tx.setDetails(details);
        tx.setTaskId(taskId);
        tx.setRewardOfferId(rewardOfferId);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        transactions.save(tx);
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        return users.findByPhoneHash(Crypto.sha256Hex(phoneNorm))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private PointsAccountView toAccountView(UserEntity user) {
        int balance = safe(user.getTotalPoints());
        int lifetime = Math.max(safe(user.getLifetimePoints()), balance);
        return new PointsAccountView(user.getId().toString(), balance, lifetime, safe(user.getCompletedTasksCount()), titleFor(lifetime));
    }

    public String titleFor(int points) {
        if (points >= 3000) return "Легенда добрых маршрутов";
        if (points >= 2500) return "Мастер доступного города";
        if (points >= 2000) return "Навигатор перемен";
        if (points >= 1500) return "Хранитель маршрутов";
        if (points >= 1000) return "Герой района";
        if (points >= 500) return "Проводник добра";
        if (points >= 100) return "Разведчик тротуаров";
        return "Новичок GoodRoad";
    }

    private static int safe(Integer value) { return value == null ? 0 : Math.max(0, value); }

    public static PointTransactionView toView(PointTransactionEntity tx) {
        return new PointTransactionView(
                tx.getId() == null ? null : tx.getId().toString(),
                tx.getDirection(), tx.getAmount(), tx.getReason(), tx.getDetails(),
                tx.getTaskId() == null ? null : tx.getTaskId().toString(),
                tx.getRewardOfferId() == null ? null : tx.getRewardOfferId().toString(),
                tx.getSourceType(), tx.getSourceId() == null ? null : tx.getSourceId().toString(),
                tx.getCreatedAt()
        );
    }
}
