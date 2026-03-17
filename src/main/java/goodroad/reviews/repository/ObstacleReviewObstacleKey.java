package GoodRoad.reviews.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ObstacleReviewObstacleKey implements Serializable { // аналогично с UserObstaclePolicyKey, пара ключей не должна дублироваться

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "obstacle_type", nullable = false, length = 32)
    private String obstacleType;

    public ObstacleReviewObstacleKey() {
    }

    public ObstacleReviewObstacleKey(Long reviewId, String obstacleType) {
        this.reviewId = reviewId;
        this.obstacleType = obstacleType;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getObstacleType() {
        return obstacleType;
    }

    public void setObstacleType(String obstacleType) {
        this.obstacleType = obstacleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObstacleReviewObstacleKey that)) {
            return false;
        }
        return Objects.equals(reviewId, that.reviewId) && Objects.equals(obstacleType, that.obstacleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId, obstacleType);
    }
}
