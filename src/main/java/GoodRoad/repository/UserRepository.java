package GoodRoad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import GoodRoad.model.user;

public interface UserRepository extends JpaRepository<user, Long> {
}
