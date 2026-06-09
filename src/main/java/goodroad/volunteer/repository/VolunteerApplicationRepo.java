package goodroad.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VolunteerApplicationRepo extends JpaRepository<VolunteerApplicationEntity, Long> {
    Optional<VolunteerApplicationEntity> findFirstByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    List<VolunteerApplicationEntity> findByStatusOrderByCreatedAtAsc(String status);
}
