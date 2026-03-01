package GoodRoad.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService svc;

    public AuthController(AuthService svc) {
        this.svc = svc;
    }

    // ниже модели данных для входящих запросов и исходящих ответов API

    public record RegisterReq(String firstName, String lastName, @NotBlank String phone, @NotBlank String password) {}

    public record LoginReq(@NotBlank String phone, @NotBlank String password) {}

    public record ChangePassReq(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    public record UserView(String id, String role) {}

    public record AuthResp(UserView user) {}

    @PostMapping("/register")
    public AuthResp register(@Valid @RequestBody RegisterReq req) {
        return svc.register(req);
    }

    @PostMapping("/login")
    public AuthResp login(@Valid @RequestBody LoginReq req) {
        return svc.login(req);
    }

    @PostMapping("/change-password")
    public void changePass(Authentication auth, @Valid @RequestBody ChangePassReq req) {
        svc.changePass(auth.getName(), req);
    }
}