package goodroad.reviews.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface ObstacleReviewPhotoRepo extends JpaRepository<ObstacleReviewPhotoEntity, Long> {
    List<ObstacleReviewPhotoEntity> findByReviewId(Long reviewId);

    List<ObstacleReviewPhotoEntity> findByReviewIdIn(Collection<Long> reviewIds);

    boolean existsByReviewId(Long reviewId);

    @Transactional
    void deleteByReviewId(Long reviewId);
}