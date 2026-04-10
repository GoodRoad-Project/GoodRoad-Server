package goodroad.users.users;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserSettingsService service;

    public UserController(UserSettingsService service) {
        this.service = service;
    }

    @GetMapping("")
    public UserSettingsService.SettingsView getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.getCurrentUser(currentUsername);
    }

    @PutMapping("")
    public UserSettingsService.SettingsView updateCurrentUser(@RequestBody UserSettingsService.UpdateSettingsReq req) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.updateCurrentUserSettings(currentUsername, req);
    }

    @PostMapping("")
    public void changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.changePassword(currentUsername, oldPassword, newPassword);
    }

    @DeleteMapping("")
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