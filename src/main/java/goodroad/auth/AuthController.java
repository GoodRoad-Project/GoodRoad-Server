package goodroad.auth;

import jakarta.validation.Valid;
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
            @Valid @RequestBody AuthService.RegisterReq req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthService.AuthResp login(
            @Valid @RequestBody AuthService.LoginReq req) {
        return service.login(req);
    }

    @PostMapping("/refresh")
    public AuthService.AuthResp refresh(
            @Valid @RequestBody AuthService.RefreshReq req) {
        return service.refresh(req);
    }

    @PostMapping("/recover-password")
    public void recoverPassword(
            @RequestBody AuthService.RecoverPassReq req) {
        service.recoverPass(req.phone(), req.firstName(), req.lastName(), req.newPassword());
    }
}