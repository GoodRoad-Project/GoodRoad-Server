package GoodRoad.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByPhoneHash(String phoneHash);

    @Modifying
    @Query("""
        delete from UserEntity u
        where u.role <> 'MODERATOR_ADMIN'
          and u.active = false
          and u.lastActiveAt < :cutoff
       """)
    int deleteInactiveBefore(@Param("cutoff") Instant cutoff);
}