package goodroad.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VolunteerComplaintRepo extends JpaRepository<VolunteerComplaintEntity, Long> {
    List<VolunteerComplaintEntity> findByStatusOrderByCreatedAtAsc(String status);
}
