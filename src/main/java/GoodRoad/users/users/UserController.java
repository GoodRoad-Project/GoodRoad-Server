package GoodRoad.users.users;

import GoodRoad.users.repository.UserSettingsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserSettingsService service;

    public UserController(UserSettingsService service) {
        this.service = service;
    }

    @DeleteMapping("") //TODO: может, получше назвать роут
    public void deleteCurrentUser(@RequestBody UserSettingsService.DeleteAccountReq req) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.deleteCurrent(currentUsername, req);
    }

    @DeleteMapping("/{id}")
    public void deleteUserByAdmin(@PathVariable String id, @RequestBody UserSettingsService.DeleteAccountReq req) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.deleteByAdmin(currentUsername, id, req);
    }
}