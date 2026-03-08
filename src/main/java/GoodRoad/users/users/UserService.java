package GoodRoad.users.users;

import GoodRoad.api.ApiErrors;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import GoodRoad.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepo users;

    public UserService(UserRepo users) {
        this.users = users;
    }

    public void deleteCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName(); //вернет логин
        System.out.println("AUTH USERNAME = " + currentUsername);

        UserEntity user = users.findByPhoneHash(currentUsername)
                .orElseThrow(() -> new ApiErrors.ApiException(HttpStatus.NOT_FOUND, "NO_USER", "User not found"));

        if (!Role.USER.name().equals(user.getRole())) {
            throw new ApiErrors.ApiException(
                    HttpStatus.FORBIDDEN,
                    "CANT_DELETE",
                    "Only regular users can delete their account"
            );
        }

        users.delete(user);
    }


}
