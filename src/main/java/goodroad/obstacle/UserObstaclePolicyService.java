package goodroad.obstacle;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ObstacleType;
import goodroad.users.repository.*;
import goodroad.security.Crypto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

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
    public List<PolicyItem> getUserObstaclePolicies(String phoneFromAuth) {
        UserEntity user = findCurrentUser(phoneFromAuth);
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

    private List<PolicyItem> getCurrentUser(String phoneFromAuth) {
        return getUserObstaclePolicies(phoneFromAuth);
    }

    @Transactional
    public List<PolicyItem> replaceUserObstaclePolicies(String phoneFromAuth, ReplacePolicyReq req) {
        if (req == null || req.items() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_OBSTACLE_POLICY_INVALID", "User obstacle policy is invalid");
        }

        UserEntity user = findCurrentUser(phoneFromAuth);
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
                throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_SEVERITY_INVALID", "Obstacle severity is invalid");
            }

            normalized.put(type, new PolicyItem(type, true, severity));
        }

        for (PolicyItem item : normalized.values()) {
            UserObstaclePolicyEntity entity = UserObstaclePolicyEntity.builder()
                    .id(new UserObstaclePolicyKey(userId, item.obstacleType()))
                    .maxAllowedSeverity(item.maxAllowedSeverity())
                    .build();
            policies.save(entity);
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);
        return getCurrentUser(phoneFromAuth);
    }

    private UserEntity findCurrentUser(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone number not found");
        }
        String phoneHash = Crypto.sha256Hex(phoneNorm);
        return users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone number not found"));
    }
}