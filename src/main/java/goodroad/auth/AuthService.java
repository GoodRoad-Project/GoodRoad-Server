package goodroad.auth;

import goodroad.api.ApiErrors.ApiException;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.security.Crypto;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

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
    ) {}

    public record LoginReq(
            @NotBlank String phone,
            @NotBlank String password
    ) {}

    public record UserView(
            String id,
            String role
    ) {}

    public record AuthResp(
            UserView user
    ) {}

    @Transactional
    public AuthResp register(RegisterReq req) {

        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "USER_PHONE_INVALID",
                    "Phone number is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "USER_PHONE_ALREADY_EXISTS",
                    "Phone number already exists");
        }

        Instant now = Instant.now();

        UserEntity user = UserEntity.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phoneHash(phoneHash)
                .role("USER")
                .passHash(passwordEncoder.encode(req.password()))
                .active(true)
                .createdAt(now)
                .lastActiveAt(now)
                .build();

        UserEntity saved = users.save(user);   // ✅ ВАЖНО

        return toResp(saved);
    }

    @Transactional
    public AuthResp login(LoginReq req) {

        String phoneNorm = Crypto.normPhone(req.phone());
        String phoneHash = Crypto.sha256Hex(phoneNorm);

        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_INVALID_CREDENTIALS_PHONE",
                        "Phone number is invalid"
                ));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "USER_INACTIVE",
                    "User account is inactive");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_CREDENTIALS",
                    "Password is invalid");
        }

        user.setLastActiveAt(Instant.now());
        UserEntity saved = users.save(user);

        return toResp(saved);
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
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "USER_PHONE_NOT_FOUND",
                    "User with given phone number not found");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);

        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_PHONE_NOT_FOUND",
                        "User with given phone number not found"
                ));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "USER_INACTIVE",
                    "User account is inactive");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "USER_OLD_PASS_INVALID",
                    "Old password is invalid");
        }

        user.setPassHash(passwordEncoder.encode(newPassword));
        user.setLastActiveAt(Instant.now());

        users.save(user);
    }

    private AuthResp toResp(UserEntity user) {
        return new AuthResp(
                new UserView(
                        String.valueOf(Objects.requireNonNull(user.getId())),
                        user.getRole()
                )
        );
    }
}