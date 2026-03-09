package GoodRoad.obstacle;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.model.ObstacleType;
import GoodRoad.users.repository.UserObstaclePolicyEntity;
import GoodRoad.users.repository.UserObstaclePolicyKey;
import GoodRoad.users.repository.UserObstaclePolicyRepo;
import GoodRoad.security.Crypto;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
@Service
public class UserObstaclePolicyService { // тут храним логику для избегаемых препятствий

    private final UserRepo users;
    private final UserObstaclePolicyRepo policies;

    public UserObstaclePolicyService(UserRepo users, UserObstaclePolicyRepo policies) {
        this.users = users;
        this.policies = policies;
    }

    public record PolicyItem(String obstacleType, boolean selected, Short maxAllowedSeverity) {
    }

    public record ReplacePolicyReq(List<PolicyItem> items) {
    }

    @Transactional(readOnly = true)
    public List<PolicyItem> getCurrent(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        Map<String, Short> byType = new HashMap<>();
        for (UserObstaclePolicyEntity entity : policies.findByIdUserId(user.getId())) {
            byType.put(entity.getId().getObstacleType(), entity.getMaxAllowedSeverity());
        }

        List<PolicyItem> out = new ArrayList<>();
        for (String type : ObstacleType.allNames()) {
            out.add(new PolicyItem(type, byType.containsKey(type), byType.get(type)));
        }
        return out;
    }

    @Transactional
    public List<PolicyItem> replaceCurrent(String phoneFromAuth, ReplacePolicyReq req) {
        if (req == null || req.items() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_POLICY", "Bad policy");
        }

        UserEntity user = findCurrent(phoneFromAuth);
        Long userId = user.getId();
        policies.deleteByIdUserId(userId);

        Map<String, PolicyItem> normalized = new HashMap<>();
        for (PolicyItem item : req.items()) {
            String type = ObstacleType.normalize(item.obstacleType());
            Short severity = item.maxAllowedSeverity();

            if (!item.selected()) {
                continue;
            }
            if (severity == null || severity < 1 || severity > 3) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_POLICY_SEVERITY", "Bad policy severity");
            }

            normalized.put(type, new PolicyItem(type, true, severity));
        }

        for (PolicyItem item : normalized.values()) {
            UserObstaclePolicyEntity entity = new UserObstaclePolicyEntity();
            entity.setId(new UserObstaclePolicyKey(userId, item.obstacleType()));
            entity.setMaxAllowedSeverity(item.maxAllowedSeverity());
            policies.save(entity);
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);
        return getCurrent(phoneFromAuth);
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user");
        }
        String phoneHash = Crypto.sha256Hex(phoneNorm);
        return users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user"));
    }
}