package GoodRoad.auth;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthService.AuthResp register(
            @RequestBody AuthService.RegisterReq req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthService.AuthResp login(
            @RequestBody AuthService.LoginReq req) {
        return service.login(req);
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody AuthService.ChangePassReq req) {
        service.changePass(req.phoneFromAuth(), req.oldPassword(), req.newPassword());
    }
}