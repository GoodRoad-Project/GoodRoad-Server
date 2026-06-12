package goodroad.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HelpRequestRepo extends JpaRepository<HelpRequestEntity, Long> {
    List<HelpRequestEntity> findByRequesterIdOrderByDateDescTimeDescCreatedAtDesc(Long requesterId);
    List<HelpRequestEntity> findByVolunteerIdOrderByDateDescTimeDescCreatedAtDesc(Long volunteerId);
    List<HelpRequestEntity> findByStatusOrderByDateAscTimeAscCreatedAtAsc(String status);
    List<HelpRequestEntity> findByRequesterIdAndStatus(Long requesterId, String status);
    List<HelpRequestEntity> findByVolunteerIdAndStatus(Long volunteerId, String status);
}
