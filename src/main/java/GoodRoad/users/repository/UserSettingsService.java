package GoodRoad.users.repository;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.auth.AuthService;
import GoodRoad.security.Crypto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
@Service
public class UserSettingsService {

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserSettingsService(UserRepo users, PasswordEncoder passwordEncoder, AuthService authService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public record SettingsView(
            String id,
            String role,
            String firstName,
            String lastName,
            String photoUrl,
            boolean active
    ) {
    }

    public record UpdateSettingsReq(
            String firstName,
            String lastName,
            String photoUrl,
            String phone
    ) {
    }

    public record DeleteAccountReq(
            String password
    ) {
    }

    @Transactional(readOnly = true)
    public SettingsView getCurrent(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        return toView(user);
    }

    @Transactional
    public SettingsView updateCurrentUserSettings(String phoneFromAuth, UpdateSettingsReq req) {
        if (req == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_UPDATE_EMPTY", "No fields provided to update");
        }

        String firstName = blankToNull(req.firstName());
        String lastName = blankToNull(req.lastName());
        String photoUrl = blankToNull(req.photoUrl());
        String phone = blankToNull(req.phone());

        if (firstName == null && lastName == null && photoUrl == null && phone == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_UPDATE_EMPTY", "No fields provided to update");
        }

        UserEntity user= findCurrent(phoneFromAuth);

        if (req.firstName() != null) {
            user.setFirstName(firstName);
        }
        if (req.lastName() != null) {
            user.setLastName(lastName);
        }
        if (req.photoUrl() != null) {
            user.setPhotoUrl(photoUrl);
        }
        if (req.phone() != null) {
            String newPhoneNorm = Crypto.normPhone(req.phone());
            if (newPhoneNorm.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_INVALID", "Phone number is invalid");
            }

            String newPhoneHash = Crypto.sha256Hex(newPhoneNorm);
            users.findByPhoneHash(newPhoneHash)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_USED", "Phone number already used");
                    });

            user.setPhoneHash(newPhoneHash);
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);
        return toView(user);
    }

    @Transactional
    public void changePassword(String phoneFromAuth, String oldPassword, String newPassword) {
        authService.changePass(phoneFromAuth, oldPassword, newPassword);
    }

    @Transactional
    public void deleteCurrent(String phoneFromAuth, DeleteAccountReq req) {
        if (req == null || req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_INVALID", "Password is invalid");
        }

        UserEntity user= findCurrent(phoneFromAuth);
        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENTIALS_INVALID", "Credentials are invalid");
        }

        users.delete(user);
    }

    private SettingsView toView(UserEntity user) {
        return new SettingsView(
                user.getId().toString(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhotoUrl(),
                user.isActive()
        );
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        return users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }
}