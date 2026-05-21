package goodroad.obstacle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.List;

public interface ObstacleFeatureObstacleScoreRepo extends JpaRepository<ObstacleFeatureObstacleScoreEntity, ObstacleFeatureObstacleScoreKey> {
    List<ObstacleFeatureObstacleScoreEntity> findByIdFeatureIdIn(Collection<Long> featureIds);

    @Transactional
    void deleteByIdFeatureId(Long featureId);
}