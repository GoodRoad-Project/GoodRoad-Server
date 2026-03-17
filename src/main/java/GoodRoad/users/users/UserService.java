package GoodRoad.users.users;

import GoodRoad.api.ApiErrors;
import GoodRoad.security.Crypto;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import GoodRoad.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepo users;

    public UserService(UserRepo users) {
        this.users = users;
    }

    public void deleteCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName(); //вернет логин
        log.info("AUTH USERNAME = {}", currentUsername);

        UserEntity user = users.findByPhoneHash(Crypto.sha256Hex(Crypto.normPhone(currentUsername)))
                .orElseThrow(() -> new ApiErrors.ApiException(HttpStatus.NOT_FOUND, "USER_PHONE_NOT_FOUND", "User with given phone number not found"));

        if (!Role.USER.name().equals(user.getRole())) {
            throw new ApiErrors.ApiException(
                    HttpStatus.FORBIDDEN,
                    "USER_CANT_DELETE",
                    "Only regular users can delete their account"
            );
        }

        users.delete(user);
    }
}
