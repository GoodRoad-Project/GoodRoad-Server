package goodroad.users.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhoneHash(String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.phoneHash = :phoneHash")
    Optional<UserEntity> findByPhoneHashForUpdate(@Param("phoneHash") String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

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
