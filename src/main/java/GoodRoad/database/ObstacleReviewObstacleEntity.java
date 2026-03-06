package GoodRoad.database;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "obstacle_review_obstacle")
public class ObstacleReviewObstacleEntity { // нужно для списка препядствий, которые отмечаем в отзыве

    @EmbeddedId
    private ObstacleReviewObstacleKey id;

    public ObstacleReviewObstacleKey getId() {
        return id;
    }

    public void setId(ObstacleReviewObstacleKey id) {
        this.id = id;
    }
}
