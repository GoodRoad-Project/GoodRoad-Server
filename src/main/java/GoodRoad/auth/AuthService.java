package GoodRoad.auth;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import GoodRoad.security.Crypto;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepo users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterReq(
            String firstName,
            String lastName,
            @NotBlank String phone,
            @NotBlank String password
    ) {
    }

    public record LoginReq(
            @NotBlank String phone,
            @NotBlank String password
    ) {
    }

    public record UserView(
            String id,
            String role
    ) {
    }

    public record AuthResp(
            UserView user
    ) {
    }

    @Transactional
    public AuthResp register(RegisterReq req) {
        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_INVALID", "Phone number is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "Phone number already exists");
        }

        Instant now = Instant.now();

        UserEntity u = new UserEntity();
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setPhoneHash(phoneHash);
        u.setRole("USER");
        u.setPassHash(passwordEncoder.encode(req.password()));
        u.setActive(true);
        u.setCreatedAt(now);
        u.setLastActiveAt(now);

        users.save(u);
        return toResp(u);
    }

    @Transactional
    public AuthResp login(LoginReq req) {
        String phoneNorm = Crypto.normPhone(req.phone());
        String phoneHash = Crypto.sha256Hex(phoneNorm);

        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Invalid phone"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "Account inactive");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Invalid password");
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return toResp(user);
    }

    public record ChangePassReq(
            String phoneFromAuth,
            String oldPassword,
            String newPassword
    ) {}

    @Transactional
    public void changePass(String phoneFromAuth, String oldPassword, String newPassword) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        UserEntity u = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user"));

        if (!u.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INACTIVE", "Account inactive");
        }
        if (!passwordEncoder.matches(oldPassword, u.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_OLD_PASS", "Bad old password");
        }

        u.setPassHash(passwordEncoder.encode(newPassword));
        u.setLastActiveAt(Instant.now());
        users.save(u);
    }

    private AuthResp toResp(UserEntity u) {
        return new AuthResp(new UserView(u.getId().toString(), u.getRole()));
    }
}