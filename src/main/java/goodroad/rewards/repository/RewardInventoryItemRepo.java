package goodroad.rewards.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RewardInventoryItemRepo extends JpaRepository<RewardInventoryItemEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select item from RewardInventoryItemEntity item
        where item.rewardOfferId = :offerId
          and item.status = 'AVAILABLE'
          and (item.validUntil is null or item.validUntil > :now)
        order by item.id
        """)
    List<RewardInventoryItemEntity> findAvailableForUpdate(
            @Param("offerId") Long offerId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
        select count(item) from RewardInventoryItemEntity item
        where item.rewardOfferId = :offerId
          and item.status = 'AVAILABLE'
          and (item.validUntil is null or item.validUntil > :now)
        """)
    long countAvailable(@Param("offerId") Long offerId, @Param("now") Instant now);
}
