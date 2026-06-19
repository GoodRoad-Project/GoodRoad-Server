package goodroad.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserTaskTargetCompletionRepo extends JpaRepository<UserTaskTargetCompletionEntity, Long> {
    boolean existsByUserIdAndTargetId(Long userId, Long targetId);
    long countByUserIdAndTaskId(Long userId, Long taskId);
    List<UserTaskTargetCompletionEntity> findByUserIdAndTaskIdIn(Long userId, Collection<Long> taskIds);
}
