package goodroad.reviews.repository;

import jakarta.persistence.*;
import lombok.Builder;
import java.time.Instant;

@Entity
@Table(name = "obstacle_review_photo")
public class ObstacleReviewPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ObstacleReviewPhotoEntity() {
    }

    @Builder
    public ObstacleReviewPhotoEntity(
            Long reviewId,
            String url,
            Instant createdAt
    ) {
        this.reviewId = reviewId;
        this.url = url;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
