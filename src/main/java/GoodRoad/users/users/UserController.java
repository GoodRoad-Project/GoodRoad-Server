package GoodRoad.users.users;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @DeleteMapping("") //TODO: может, получше назвать роут
    public void deleteCurrentUser() {
        service.deleteCurrentUser();
    }
}
