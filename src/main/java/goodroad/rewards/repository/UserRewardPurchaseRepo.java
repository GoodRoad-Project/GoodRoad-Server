package goodroad.rewards.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface UserRewardPurchaseRepo extends JpaRepository<UserRewardPurchaseEntity, Long> {
    List<UserRewardPurchaseEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchase from UserRewardPurchaseEntity purchase where purchase.id = :id and purchase.userId = :userId")
    java.util.Optional<UserRewardPurchaseEntity> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
