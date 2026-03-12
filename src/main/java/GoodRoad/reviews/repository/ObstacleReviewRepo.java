package GoodRoad.reviews.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ObstacleReviewRepo extends JpaRepository<ObstacleReviewEntity, Long> { // для фото отзывов
    Page<ObstacleReviewEntity> findByStatus(String status, Pageable pageable);

    @Query("""
            select r from ObstacleReviewEntity r
            where r.status = :status
              and (r.takenByModeratorId is null or r.takenByModeratorId = :moderatorId)
            order by r.createdAt asc
            """)
    Page<ObstacleReviewEntity> findQueueForModerator(String status, Long moderatorId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ObstacleReviewEntity r where r.id = :id")
    Optional<ObstacleReviewEntity> findByIdForUpdate(Long id);

    Optional<ObstacleReviewEntity> findByFeatureIdAndAuthorId(Long featureId, Long authorId);

    Optional<ObstacleReviewEntity> findByIdAndAuthorId(Long id, Long authorId);

    List<ObstacleReviewEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<ObstacleReviewEntity> findByFeatureIdAndStatusOrderByCreatedAtDesc(Long featureId, String status);

    long countByFeatureIdAndStatus(Long featureId, String status);

    long countByAuthorIdAndStatus(Long authorId, String status);

    @Query("select avg(r.severity) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Double avgSeverity(Long featureId, String status);

    @Query("select max(r.createdAt) from ObstacleReviewEntity r where r.featureId = :featureId and r.status = :status")
    Instant lastAt(Long featureId, String status);
}