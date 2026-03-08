package GoodRoad.reviews.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ObstacleReviewPhotoRepo extends JpaRepository<ObstacleReviewPhotoEntity, UUID> {
    List<ObstacleReviewPhotoEntity> findByReviewId(UUID reviewId);

    List<ObstacleReviewPhotoEntity> findByReviewIdIn(Collection<UUID> reviewIds);

    @Transactional
    void deleteByReviewId(UUID reviewId);
}
