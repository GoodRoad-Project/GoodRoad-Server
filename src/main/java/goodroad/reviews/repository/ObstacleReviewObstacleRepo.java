package goodroad.reviews.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ObstacleReviewObstacleRepo extends JpaRepository<ObstacleReviewObstacleEntity, ObstacleReviewObstacleKey> { // связываем отзыв и препятствия
    List<ObstacleReviewObstacleEntity> findByIdReviewId(Long reviewId);

    List<ObstacleReviewObstacleEntity> findByIdReviewIdIn(Collection<Long> reviewIds);

    @Transactional
    void deleteByIdReviewId(Long reviewId);
}
