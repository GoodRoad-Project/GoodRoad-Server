package goodroad.obstacle.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ObstacleFeatureObstacleScoreKey implements Serializable {

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "obstacle_type", nullable = false, length = 32)
    private String obstacleType;

    public ObstacleFeatureObstacleScoreKey() {
    }

    public ObstacleFeatureObstacleScoreKey(Long featureId, String obstacleType) {
        this.featureId = featureId;
        this.obstacleType = obstacleType;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public void setFeatureId(Long featureId) {
        this.featureId = featureId;
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
        if (!(o instanceof ObstacleFeatureObstacleScoreKey that)) {
            return false;
        }
        return Objects.equals(featureId, that.featureId) && Objects.equals(obstacleType, that.obstacleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureId, obstacleType);
    }
}