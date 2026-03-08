package GoodRoad.users.repository;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_obstacle_policy")
public class UserObstaclePolicyEntity {

    @EmbeddedId
    private UserObstaclePolicyKey id;

    @Column(name = "max_allowed_severity")
    private Short maxAllowedSeverity;

    public UserObstaclePolicyKey getId() {
        return id;
    }

    public void setId(UserObstaclePolicyKey id) {
        this.id = id;
    }

    public Short getMaxAllowedSeverity() {
        return maxAllowedSeverity;
    }

    public void setMaxAllowedSeverity(Short maxAllowedSeverity) {
        this.maxAllowedSeverity = maxAllowedSeverity;
    }
}
