package goodroad.obstacle.repository;

import jakarta.persistence.*;
import lombok.Builder;

@Entity
@Table(name = "obstacle_feature_obstacle_score")
public class ObstacleFeatureObstacleScoreEntity {

    @EmbeddedId
    private ObstacleFeatureObstacleScoreKey id;

    @Column(name = "severity_estimate", nullable = false)
    private short severityEstimate;

    @Column(name = "reviews_count", nullable = false)
    private int reviewsCount;

    public ObstacleFeatureObstacleScoreEntity() {
    }

    @Builder
    public ObstacleFeatureObstacleScoreEntity(
            ObstacleFeatureObstacleScoreKey id,
            short severityEstimate,
            int reviewsCount
    ) {
        this.id = id;
        this.severityEstimate = severityEstimate;
        this.reviewsCount = reviewsCount;
    }

    public ObstacleFeatureObstacleScoreKey getId() {
        return id;
    }

    public void setId(ObstacleFeatureObstacleScoreKey id) {
        this.id = id;
    }

    public short getSeverityEstimate() {
        return severityEstimate;
    }

    public void setSeverityEstimate(short severityEstimate) {
        this.severityEstimate = severityEstimate;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}