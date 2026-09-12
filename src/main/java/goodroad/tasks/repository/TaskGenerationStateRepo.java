package goodroad.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskGenerationStateRepo extends JpaRepository<TaskGenerationStateEntity, Long> {
}
