package goodroad.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhoneHash(String phoneHash);

    List<UserEntity> findByRoleIn(List<String> roles);

    @Modifying
    @Query("""
        delete from UserEntity user
        where user.role <> 'MODERATOR_ADMIN'
          and user.active = false
          and user.lastActiveAt < :cutoff
       """)
    int deleteInactiveBefore(@Param("cutoff") Instant cutoff);
}