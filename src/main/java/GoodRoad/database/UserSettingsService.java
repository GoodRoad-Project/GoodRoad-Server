package GoodRoad.database;

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
    private final PasswordEncoder pe;
    private final AuthService authService;

    public UserSettingsService(UserRepo users, PasswordEncoder pe, AuthService authService) {
        this.users = users;
        this.pe = pe;
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
        UserEntity u = findCurrent(phoneFromAuth);
        return toView(u);
    }

    @Transactional
    public SettingsView updateCurrent(String phoneFromAuth, UpdateSettingsReq req) {
        if (req == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_UPDATE", "Nothing to update");
        }

        String firstName = blankToNull(req.firstName());
        String lastName = blankToNull(req.lastName());
        String photoUrl = blankToNull(req.photoUrl());
        String phone = blankToNull(req.phone());

        if (firstName == null && lastName == null && photoUrl == null && phone == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_UPDATE", "Nothing to update");
        }

        UserEntity u = findCurrent(phoneFromAuth);

        if (req.firstName() != null) {
            u.setFirstName(firstName);
        }
        if (req.lastName() != null) {
            u.setLastName(lastName);
        }
        if (req.photoUrl() != null) {
            u.setPhotoUrl(photoUrl);
        }
        if (req.phone() != null) {
            String newPhoneNorm = Crypto.normPhone(req.phone());
            if (newPhoneNorm.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PHONE", "Bad phone");
            }

            String newPhoneHash = Crypto.sha256Hex(newPhoneNorm);
            users.findByPhoneHash(newPhoneHash)
                    .filter(other -> !other.getId().equals(u.getId()))
                    .ifPresent(other -> {
                        throw new ApiException(HttpStatus.CONFLICT, "PHONE_USED", "Phone already used");
                    });

            u.setPhoneHash(newPhoneHash);
        }

        u.setLastActiveAt(Instant.now());
        users.save(u);
        return toView(u);
    }

    @Transactional
    public void changePassword(String phoneFromAuth, String oldPassword, String newPassword) {
        authService.changePass(phoneFromAuth, oldPassword, newPassword);
    }

    @Transactional
    public void deleteCurrent(String phoneFromAuth, DeleteAccountReq req) {
        if (req == null || req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PASSWORD", "Bad password");
        }

        UserEntity u = findCurrent(phoneFromAuth);
        if (!pe.matches(req.password(), u.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDS", "Bad credentials");
        }

        users.delete(u);
    }

    private SettingsView toView(UserEntity u) {
        return new SettingsView(
                u.getId().toString(),
                u.getRole(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhotoUrl(),
                u.isActive()
        );
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        return users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user"));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }
}