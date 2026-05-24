package goodroad.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SosNotificationRepo extends JpaRepository<SosNotificationEntity, Long> {
    List<SosNotificationEntity> findAllByOrderByCreatedAtDesc();
    List<SosNotificationEntity> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}
