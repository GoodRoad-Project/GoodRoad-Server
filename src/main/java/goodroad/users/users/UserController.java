package goodroad.users.users;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProfileService service;

    public UserController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("")
    public UserProfileService.ProfileView getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.getCurrentUser(currentUsername);
    }

    @PutMapping("")
    public UserProfileService.ProfileView updateProfile(
            @RequestBody UserProfileService.UpdateProfileReq req
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.updateProfile(currentUsername, req);
    }

    @PutMapping("/phone")
    public UserProfileService.ProfileView changePhone(
            @RequestBody UserProfileService.ChangePhoneReq req
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.changePhone(currentUsername, req);
    }

    @PostMapping("")
    public void changePassword(
            @RequestBody UserProfileService.ChangePasswordReq req
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.changePassword(currentUsername, req);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileService.AvatarUploadResp uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.uploadAvatar(currentUsername, file);
    }

    @DeleteMapping("")
    public void deleteCurrentUser(
            @RequestBody UserProfileService.DeleteAccountReq req
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.deleteCurrent(currentUsername, req);
    }

    @DeleteMapping("/{id}")
    public void deleteUserByAdmin(
            @PathVariable String id,
            @RequestBody UserProfileService.DeleteAccountReq req
    ) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        service.deleteByAdmin(currentUsername, id, req);
    }
}