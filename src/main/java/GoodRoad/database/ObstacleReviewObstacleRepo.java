package GoodRoad.database;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ObstacleReviewObstacleRepo extends JpaRepository<ObstacleReviewObstacleEntity, ObstacleReviewObstacleKey> { // связываем отзыв и препятствия
    List<ObstacleReviewObstacleEntity> findByIdReviewId(UUID reviewId);

    List<ObstacleReviewObstacleEntity> findByIdReviewIdIn(Collection<UUID> reviewIds);

    @Transactional
    void deleteByIdReviewId(UUID reviewId);
}
