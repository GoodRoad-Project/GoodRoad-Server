package goodroad.points;

import goodroad.api.ApiErrors.ApiException;
import goodroad.points.repository.*;
import goodroad.users.repository.*;
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

    public record PointTransactionView(String id, String direction, int amount, String reason, String details, String taskId, String rewardOfferId, String sourceType, String sourceId, java.time.Instant createdAt) {}

    @Transactional
    public void earn(UserEntity user, int amount, String reason, String details, Long taskId, String sourceType, Long sourceId) {
        if (amount <= 0) return;
        user.setTotalPoints(safe(user.getTotalPoints()) + amount);
        user.setLifetimePoints(safe(user.getLifetimePoints()) + amount);
        users.save(user);
        saveTx(user.getId(), "EARN", amount, reason, details, taskId, null, sourceType, sourceId);
    }

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
    public List<PointTransactionView> history(UserEntity user) {
        return transactions.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(PointLedgerService::toView).toList();
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
