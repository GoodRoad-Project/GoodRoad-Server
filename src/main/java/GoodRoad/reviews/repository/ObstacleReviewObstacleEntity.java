package GoodRoad.reviews.repository;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "obstacle_review_obstacle")
public class ObstacleReviewObstacleEntity { // нужно для списка препятствий, которые отмечаем в отзыве

    @EmbeddedId
    private ObstacleReviewObstacleKey id;

    @Column(name = "severity", nullable = false)
    private short severity;

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