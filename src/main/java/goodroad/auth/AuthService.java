package goodroad.auth;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.security.JwtService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.validation.InputRules;
import io.jsonwebtoken.Claims;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern CYRILLIC_NAME_PATTERN = Pattern.compile("^(?=.*\\p{IsCyrillic})[\\p{IsCyrillic} -]+$");

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepo users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public record RecoverPassReq(
            @NotBlank String phone,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String newPassword
    ) {
    }

    public record RefreshReq(
            @NotBlank String refreshToken
    ) {
    }

    public record UserView(
            String id,
            String role
    ) {
    }

    public record AuthResp(
            UserView user,
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
        public AuthResp(UserView user, String accessToken, String tokenType) {
            this(user, accessToken, null, tokenType);
        }
    }

    @Transactional
    public AuthResp register(RegisterReq req) {
        String firstName = InputRules.requireCyrillicText(
                req.firstName(),
                "USER_FIRST_NAME_INVALID",
                "First name"
        );
        String lastName = InputRules.requireCyrillicText(
                req.lastName(),
                "USER_LAST_NAME_INVALID",
                "Last name"
        );

        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "USER_PHONE_INVALID",
                    "Phone number is invalid");
        }

        if (req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "USER_PASSWORD_INVALID",
                    "Password is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "USER_PHONE_ALREADY_EXISTS",
                    "Phone number already exists");
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

        UserEntity saved = users.save(user);

        return toResp(saved, phoneNorm);
    }

    @Transactional
    public AuthResp login(LoginReq req) {
        String phoneNorm = Crypto.normPhone(req.phone());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_CREDENTIALS_PHONE",
                    "Phone number is invalid");
        }

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

        return toResp(saved, phoneNorm);
    }


    @Transactional
    public AuthResp refresh(RefreshReq req) {
        if (req == null || req.refreshToken() == null || req.refreshToken().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_REFRESH_TOKEN_INVALID",
                    "Refresh token is invalid");
        }

        Claims claims;
        try {
            claims = jwtService.parseClaims(req.refreshToken().trim());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_REFRESH_TOKEN_INVALID",
                    "Refresh token is invalid");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_REFRESH_TOKEN_INVALID",
                    "Refresh token is invalid");
        }

        String phoneNorm = Crypto.normPhone(claims.getSubject());
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "AUTH_REFRESH_TOKEN_INVALID",
                    "Refresh token is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        UserEntity user = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_REFRESH_TOKEN_INVALID",
                        "Refresh token is invalid"
                ));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "USER_INACTIVE",
                    "User account is inactive");
        }

        user.setLastActiveAt(Instant.now());
        UserEntity saved = users.save(user);

        return toResp(saved, phoneNorm);
    }

    public record ChangePassReq(
            String phoneFromAuth,
            String oldPassword,
            String newPassword
    ) {
    }

    @Transactional
    public void changePass(String phoneFromAuth, String oldPassword, String newPassword) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "USER_PHONE_NOT_FOUND",
                    "User with given phone number not found");
        }

        if (oldPassword == null || oldPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "USER_OLD_PASS_INVALID",
                    "Old password is invalid");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "USER_NEW_PASS_INVALID",
                    "New password is invalid");
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

        if (!Objects.equals(storedFirstName, normalizedFirstName) ||
                !Objects.equals(storedLastName, normalizedLastName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_RECOVERY_DATA_INVALID", "Recovery data is invalid");
        }

        user.setPassHash(passwordEncoder.encode(newPassword));
        user.setLastActiveAt(Instant.now());
        users.save(user);
    }

    private AuthResp toResp(UserEntity user, String phoneNorm) {
        return new AuthResp(
                new UserView(
                        String.valueOf(Objects.requireNonNull(user.getId())),
                        user.getRole()
                ),
                jwtService.generateAccessToken(phoneNorm, user),
                jwtService.generateRefreshToken(phoneNorm, user),
                "Bearer"
        );
    }

    private String normalizeRecoveryName(String value) {
        String normalized = InputRules.trimToNull(value);
        if (normalized == null || !CYRILLIC_NAME_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}