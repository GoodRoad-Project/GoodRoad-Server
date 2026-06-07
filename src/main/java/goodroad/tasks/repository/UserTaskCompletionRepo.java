package goodroad.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserTaskCompletionRepo extends JpaRepository<UserTaskCompletionEntity, Long> {
    boolean existsByUserIdAndTaskId(Long userId, Long taskId);
    List<UserTaskCompletionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserTaskCompletionEntity> findByUserIdAndTaskIdIn(Long userId, Collection<Long> taskIds);
}
