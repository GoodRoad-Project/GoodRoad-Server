package GoodRoad.users.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserObstaclePolicyRepo extends JpaRepository<UserObstaclePolicyEntity, UserObstaclePolicyKey> {
    List<UserObstaclePolicyEntity> findByIdUserId(java.util.UUID userId);

    @Transactional
    void deleteByIdUserId(java.util.UUID userId);
}
