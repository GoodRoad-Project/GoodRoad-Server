package goodroad.reviews.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ObstacleReviewRepo extends JpaRepository<ObstacleReviewEntity, Long> { // для фото отзывов
    Page<ObstacleReviewEntity> findByStatus(String status, Pageable pageable);

    @Query("""
            select obstacleReview from ObstacleReviewEntity obstacleReview
            where obstacleReview.status = :status
              and (obstacleReview.takenByModeratorId is null or obstacleReview.takenByModeratorId = :moderatorId)
            order by obstacleReview.createdAt asc
            """)
    Page<ObstacleReviewEntity> findQueueForModerator(String status, Long moderatorId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select obstacleReview from ObstacleReviewEntity obstacleReview where obstacleReview.id = :id")
    Optional<ObstacleReviewEntity> findByIdForUpdate(Long id);

    Optional<ObstacleReviewEntity> findByFeatureIdAndAuthorId(Long featureId, Long authorId);

    Optional<ObstacleReviewEntity> findByIdAndAuthorId(Long id, Long authorId);

    List<ObstacleReviewEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<ObstacleReviewEntity> findByFeatureIdAndStatusOrderByCreatedAtDesc(Long featureId, String status);

    long countByFeatureIdAndStatus(Long featureId, String status);

    long countByAuthorIdAndStatus(Long authorId, String status);

    @Query("select avg(obstacleReview.severity) from ObstacleReviewEntity obstacleReview where obstacleReview.featureId = :featureId and obstacleReview.status = :status")
    Double avgSeverity(Long featureId, String status);

    @Query("select max(obstacleReview.createdAt) from ObstacleReviewEntity obstacleReview where obstacleReview.featureId = :featureId and obstacleReview.status = :status")
    Instant lastAt(Long featureId, String status);
}