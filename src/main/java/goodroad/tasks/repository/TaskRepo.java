package goodroad.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepo extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<TaskEntity> findByActivityTypeAndStatusOrderByCreatedAtDesc(String activityType, String status);
    boolean existsByTitleAndStatus(String title, String status);
}
