package goodroad.users.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserObstaclePolicyRepo extends JpaRepository<UserObstaclePolicyEntity, UserObstaclePolicyKey> {
    List<UserObstaclePolicyEntity> findByIdUserId(Long userId);

    @Transactional
    void deleteByIdUserId(Long userId);
}
