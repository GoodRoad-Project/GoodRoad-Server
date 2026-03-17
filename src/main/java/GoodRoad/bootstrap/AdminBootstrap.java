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

        UserEntity u = new UserEntity();
        u.setFirstName(null);
        u.setLastName(null);
        u.setPhoneHash(phoneHash);
        u.setRole(Role.MODERATOR_ADMIN.name());
        u.setPassHash(passwordEncoder.encode(adminPass));
        u.setPhotoUrl(null);
        u.setActive(true);
        u.setCreatedAt(now);
        u.setLastActiveAt(now);

        users.save(u);
    }
}