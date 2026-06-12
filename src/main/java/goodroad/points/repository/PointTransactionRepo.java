package goodroad.points.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepo extends JpaRepository<PointTransactionEntity, Long> {
    List<PointTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
