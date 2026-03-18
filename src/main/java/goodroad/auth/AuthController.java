package goodroad.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthService.AuthResp register(@RequestBody AuthService.RegisterReq req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthService.AuthResp login(@RequestBody AuthService.LoginReq req) {
        return service.login(req);
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody AuthService.ChangePassReq req) {
        service.changePass(req.phoneFromAuth(), req.oldPassword(), req.newPassword());
    }

    @PostMapping("/recover-password")
    public void recoverPassword(@RequestBody AuthService.RecoverPassReq req) {
        service.recoverPass(req.phone(), req.firstName(), req.lastName(), req.newPassword());
    }
}