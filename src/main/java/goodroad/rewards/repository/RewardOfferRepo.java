package goodroad.rewards.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RewardOfferRepo extends JpaRepository<RewardOfferEntity, Long> {
    @Query("""
        select reward from RewardOfferEntity reward
        where reward.active = true
          and (:minPrice is null or reward.price >= :minPrice)
          and (:maxPrice is null or reward.price <= :maxPrice)
        """)
    List<RewardOfferEntity> findActiveFiltered(@Param("minPrice") Integer minPrice, @Param("maxPrice") Integer maxPrice);
}
