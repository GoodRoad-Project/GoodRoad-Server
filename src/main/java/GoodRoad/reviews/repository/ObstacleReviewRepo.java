package GoodRoad.reviews.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ObstacleReviewRepo extends JpaRepository<ObstacleReviewEntity, Long> { // для фото отзывов
    Page<ObstacleReviewEntity> findByStatus(String status, Pageable pageable);

    Optional<ObstacleReviewEntity> findByFeatureIdAndAuthorId(Long featureId, Long authorId);

    Optional<ObstacleReviewEntity> findByIdAndAuthorId(Long id, Long authorId);

    List<ObstacleReviewEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    long countByFeatureIdAndStatus(Long featureId, String status);

    long countByAuthorIdAndStatus(Long authorId, String status);

    @Query("select avg(r.severity) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Double avgSeverity(Long featureId, String status);

    @Query("select max(r.createdAt) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Instant lastAt(Long featureId, String status);
}