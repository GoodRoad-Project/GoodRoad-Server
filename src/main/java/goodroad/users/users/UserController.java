package goodroad.users.users;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserSettingsService.AvatarUploadResp uploadAvatar(@RequestParam("file") MultipartFile file) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.uploadAvatar(currentUsername, file);
    }

    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        return service.getAvatar(fileName);
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