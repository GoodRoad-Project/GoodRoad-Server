package goodroad.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TaskTargetRepo extends JpaRepository<TaskTargetEntity, Long> {
    List<TaskTargetEntity> findByTaskIdOrderBySortOrderAscIdAsc(Long taskId);
    List<TaskTargetEntity> findByTaskIdInOrderBySortOrderAscIdAsc(Collection<Long> taskIds);
    List<TaskTargetEntity> findByTargetTypeAndTargetId(String targetType, Long targetId);
}
