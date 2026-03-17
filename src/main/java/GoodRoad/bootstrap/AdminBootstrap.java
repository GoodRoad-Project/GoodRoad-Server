package GoodRoad.bootstrap;

import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import GoodRoad.model.Role;
import GoodRoad.security.Crypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AdminBootstrap implements ApplicationRunner { // инициализируем админа-модератора при старте приложения

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final String adminPhone;
    private final String adminPass;

    public AdminBootstrap(
            UserRepo users,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.phone}") String adminPhone,
            @Value("${app.admin.pass}") String adminPass
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminPhone = adminPhone;
        this.adminPass = adminPass;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String phoneNorm = Crypto.normPhone(adminPhone);
        if (phoneNorm.isEmpty()) {
            return;
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            return;
        }

        Instant now = Instant.now();

        UserEntity user = new UserEntity();
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhoneHash(phoneHash);
        user.setRole(Role.MODERATOR_ADMIN.name());
        user.setPassHash(passwordEncoder.encode(adminPass));
        user.setPhotoUrl(null);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setLastActiveAt(now);

        users.save(user);
    }
}