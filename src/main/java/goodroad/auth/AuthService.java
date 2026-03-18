package goodroad.auth;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern CYRILLIC_NAME_PATTERN = Pattern.compile("^(?=.*\\p{IsCyrillic})[\\p{IsCyrillic} -]+$");

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

    public record ChangePassReq(
            String phoneFromAuth,
            String oldPassword,
            String newPassword
    ) {
    }

    public record RecoverPassReq(
            @NotBlank String phone,
            String firstName,
            String lastName,
            @NotBlank String newPassword
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
        String firstName = normalizeRequiredCyrillicName(
                req.firstName(),
                "USER_FIRST_NAME_INVALID",
                "First name is invalid"
        );
        String lastName = normalizeRequiredCyrillicName(
                req.lastName(),
                "USER_LAST_NAME_INVALID",
                "Last name is invalid"
        );

        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_PHONE_INVALID", "Phone number is invalid");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_PASSWORD_INVALID", "Password is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_PHONE_ALREADY_EXISTS", "Phone number already exists");
        }

        Instant now = Instant.now();

        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phoneHash(phoneHash)
                .role(Role.USER.name())
                .passHash(passwordEncoder.encode(req.password()))
                .active(true)
                .createdAt(now)
                .lastActiveAt(now)
                .build();

        users.save(user);
        return toResp(user);
    }

    @Transactional
    public AuthResp login(LoginReq req) {
        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS_PHONE", "Phone number is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS_PHONE", "Phone number is invalid"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User account is inactive");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Password is invalid");
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return toResp(user);
    }

    @Transactional
    public void changePass(String phoneFromAuth, String oldPassword, String newPassword) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone number not found");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_OLD_PASS_INVALID", "Old password is invalid");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_NEW_PASS_INVALID", "New password is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone number not found"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User account is inactive");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_OLD_PASS_INVALID", "Old password is invalid");
        }

        user.setPassHash(passwordEncoder.encode(newPassword));
        user.setLastActiveAt(Instant.now());
        users.save(user);
    }

    @Transactional
    public void recoverPass(String phone, String firstName, String lastName, String newPassword) {
        String phoneNorm = Crypto.normPhone(phone);
        String normalizedFirstName = normalizeRecoveryName(firstName);
        String normalizedLastName = normalizeRecoveryName(lastName);

        if (phoneNorm.isEmpty() || normalizedFirstName == null || normalizedLastName == null || newPassword == null || newPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid"));

        if (!user.isActive() || !Role.USER.name().equals(user.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid");
        }

        String storedFirstName = normalizeRecoveryName(user.getFirstName());
        String storedLastName = normalizeRecoveryName(user.getLastName());
        if (storedFirstName == null || storedLastName == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid");
        }

        if (!storedFirstName.equals(normalizedFirstName) || !storedLastName.equals(normalizedLastName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid");
        }

        user.setPassHash(passwordEncoder.encode(newPassword));
        user.setLastActiveAt(Instant.now());
        users.save(user);
    }

    private String normalizeRequiredCyrillicName(String value, String code, String message) {
        String normalized = trimToNull(value);
        if (normalized == null || !CYRILLIC_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, message);
        }
        return normalized;
    }

    private String normalizeRecoveryName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || !CYRILLIC_NAME_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AuthResp toResp(UserEntity user) {
        return new AuthResp(new UserView(user.getId().toString(), user.getRole()));
    }
}