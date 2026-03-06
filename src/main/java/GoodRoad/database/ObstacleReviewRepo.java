package GoodRoad.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObstacleReviewRepo extends JpaRepository<ObstacleReviewEntity, UUID> { // для фото отзывов
    Page<ObstacleReviewEntity> findByStatus(String status, Pageable pageable);

    Optional<ObstacleReviewEntity> findByFeatureIdAndAuthorId(UUID featureId, UUID authorId);

    Optional<ObstacleReviewEntity> findByIdAndAuthorId(UUID id, UUID authorId);

    List<ObstacleReviewEntity> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    long countByFeatureIdAndStatus(UUID featureId, String status);

    @Query("select avg(r.severity) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Double avgSeverity(UUID featureId, String status);

    @Query("select max(r.createdAt) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Instant lastAt(UUID featureId, String status);
}
