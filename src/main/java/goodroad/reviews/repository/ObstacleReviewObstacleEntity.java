package goodroad.reviews.repository;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;

@Entity
@Table(name = "obstacle_review_obstacle")
public class ObstacleReviewObstacleEntity { // нужно для списка препятствий, которые отмечаем в отзыве

    @EmbeddedId
    private ObstacleReviewObstacleKey id;

    @Column(name = "severity", nullable = false)
    private short severity;

    public ObstacleReviewObstacleEntity() {
    }

    @Builder
    public ObstacleReviewObstacleEntity(
            ObstacleReviewObstacleKey id,
            short severity
    ) {
        this.id = id;
        this.severity = severity;
    }

    public ObstacleReviewObstacleKey getId() {
        return id;
    }

    public void setId(ObstacleReviewObstacleKey id) {
        this.id = id;
    }

    public short getSeverity() {
        return severity;
    }

    public void setSeverity(short severity) {
        this.severity = severity;
    }
}