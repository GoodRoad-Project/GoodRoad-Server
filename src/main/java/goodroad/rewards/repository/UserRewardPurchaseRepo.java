package goodroad.rewards.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRewardPurchaseRepo extends JpaRepository<UserRewardPurchaseEntity, Long> {
    List<UserRewardPurchaseEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
