package goodroad.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.List;

public interface HelpRequestRepo extends JpaRepository<HelpRequestEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from HelpRequestEntity request where request.id = :id")
    java.util.Optional<HelpRequestEntity> findByIdForUpdate(@Param("id") Long id);

    List<HelpRequestEntity> findByRequesterIdOrderByDateDescTimeDescCreatedAtDesc(Long requesterId);
    List<HelpRequestEntity> findByVolunteerIdOrderByDateDescTimeDescCreatedAtDesc(Long volunteerId);
    List<HelpRequestEntity> findByStatusOrderByDateAscTimeAscCreatedAtAsc(String status);
    List<HelpRequestEntity> findByStatusAndDateBefore(String status, LocalDate date);
    List<HelpRequestEntity> findByStatusAndDateAndTimeBefore(String status, LocalDate date, LocalTime time);
    List<HelpRequestEntity> findByRequesterIdAndStatus(Long requesterId, String status);
    List<HelpRequestEntity> findByVolunteerIdAndStatus(Long volunteerId, String status);
}
