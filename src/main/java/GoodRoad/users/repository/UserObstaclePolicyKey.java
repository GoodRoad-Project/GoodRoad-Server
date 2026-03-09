package GoodRoad.users.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserObstaclePolicyKey implements Serializable { // Тут у нас составной ключ, нужен, т.к. запись в таблице ser_obstacle_policy уникальна по паре полей

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "obstacle_type", nullable = false, length = 32)
    private String obstacleType;

    public UserObstaclePolicyKey() {
    }

    public UserObstaclePolicyKey(Long userId, String obstacleType) {
        this.userId = userId;
        this.obstacleType = obstacleType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
        if (!(o instanceof UserObstaclePolicyKey that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(obstacleType, that.obstacleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, obstacleType);
    }
}
