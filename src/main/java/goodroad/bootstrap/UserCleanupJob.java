package goodroad.bootstrap;

import goodroad.users.repository.UserRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class UserCleanupJob { // удаляем неактивные аккаунты (отзывы остаются)

    private final UserRepo users;

    public UserCleanupJob(UserRepo users) {
        this.users = users;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanup() {
        var cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusYears(5).toInstant();
        users.deleteInactiveBefore(cutoff);
    }
}