package goodroad.users.users;

import goodroad.api.ApiErrors.ApiException;
import goodroad.auth.AuthService;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.validation.InputRules;
import goodroad.validation.TrustedUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
@Service
public class UserSettingsService {

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final StorageService storageService;
    private final TrustedUrlService trustedUrls;

    private static final Logger log = LoggerFactory.getLogger(UserSettingsService.class);

    public UserSettingsService(
            UserRepo users,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            StorageService storageService,
            TrustedUrlService trustedUrls
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.storageService = storageService;
        this.trustedUrls = trustedUrls;
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
            String photoUrl
    ) {
    }

    public record AvatarUploadResp(
            String photoUrl
    ) {
    }

    public record ChangePasswordReq(String oldPassword, String newPassword) {
    }

    public record ChangePhoneReq(
            String phone,
            String currentPassword
    ) {
    }

    public record DeleteAccountReq(
            String password
    ) {
    }

    @Transactional(readOnly = true)
    public SettingsView getCurrentUser(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        return toView(user);
    }

    @Transactional
    public SettingsView updateCurrentUserSettings(String phoneFromAuth, UpdateSettingsReq req) {
        if (req == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_UPDATE_EMPTY", "No fields provided to update");
        }

        String photoUrl = blankToNull(req.photoUrl());

        if (req.firstName() == null && req.lastName() == null && req.photoUrl() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_UPDATE_EMPTY", "No fields provided to update");
        }

        UserEntity user = findCurrent(phoneFromAuth);

        if (req.firstName() != null) {
            String firstName = InputRules.requireCyrillicText(
                    req.firstName(),
                    "USER_FIRST_NAME_INVALID",
                    "First name"
            );
            user.setFirstName(firstName);
        }

        if (req.lastName() != null) {
            String lastName = InputRules.requireCyrillicText(
                    req.lastName(),
                    "USER_LAST_NAME_INVALID",
                    "Last name"
            );
            user.setLastName(lastName);
        }

        if (req.photoUrl() != null) {
            user.setPhotoUrl(photoUrl == null ? null : trustedUrls.requireOwnedStorageUrl(
                    photoUrl,
                    "avatars",
                    user.getId(),
                    "AVATAR_URL_INVALID"
            ));
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return toView(user);
    }

    @Transactional
    public SettingsView changePhone(String phoneFromAuth, ChangePhoneReq req) {
        try {
            if (req == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PHONE_CHANGE_EMPTY",
                        "Phone change request is empty"
                );
            }

            String phone = req.phone();

            if (phone == null || phone.isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PHONE_INVALID",
                        "Phone number is invalid"
                );
            }

            UserEntity user = findCurrentForUpdate(phoneFromAuth);

            String currentPassword = req.currentPassword();

            if (currentPassword == null || currentPassword.isBlank()
                    || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                    || !passwordEncoder.matches(currentPassword, user.getPassHash())) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "CREDENTIALS_INVALID",
                        "Credentials are invalid"
                );
            }

            String newPhoneNorm = Crypto.normPhone(phone);

            if (newPhoneNorm.isEmpty()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PHONE_INVALID",
                        "Phone number is invalid"
                );
            }

            String newPhoneHash = Crypto.sha256Hex(newPhoneNorm);

            users.findByPhoneHash(newPhoneHash)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new ApiException(
                                HttpStatus.CONFLICT,
                                "PHONE_ALREADY_USED",
                                "Phone number already used"
                        );
                    });

            user.setPhoneHash(newPhoneHash);
            user.setLastActiveAt(Instant.now());
            users.save(user);

            return toView(user);
        }
        catch (Exception e) {
            log.error("Error changing phone", e); // ← Добавить это
            throw e;
        }
    }


    @Transactional
    public void changePassword(String phoneFromAuth, ChangePasswordReq req) {
        if (req == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CHANGE_EMPTY",
                    "Password change request is empty"
            );
        }

        authService.changePass(phoneFromAuth, req.oldPassword(), req.newPassword());
    }

    @Transactional
    public AvatarUploadResp uploadAvatar(String phoneFromAuth, MultipartFile file) {
        UserEntity user = findCurrent(phoneFromAuth);

        String photoUrl = storageService.uploadAvatar(file, user.getId().toString());
        user.setPhotoUrl(photoUrl);
        users.save(user);

        return new AvatarUploadResp(photoUrl);
    }

    @Transactional
    public void deleteCurrent(String phoneFromAuth, DeleteAccountReq req) {
        UserEntity user = requireCurrentWithPassword(phoneFromAuth, req);

        if (Role.MODERATOR.name().equals(user.getRole())
                || Role.MODERATOR_ADMIN.name().equals(user.getRole())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "USER_CANT_DELETE",
                    "Moderator accounts can only be deleted by an administrator"
            );
        }

        users.delete(user);
    }

    @Transactional
    public void deleteByAdmin(String phoneFromAuth, String id, DeleteAccountReq req) {
        UserEntity admin = requireCurrentWithPassword(phoneFromAuth, req);

        if (!Role.MODERATOR_ADMIN.name().equals(admin.getRole())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "USER_CANT_DELETE",
                    "Only admin can delete users"
            );
        }

        Long userId = parseId(id);

        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_ID_NOT_FOUND",
                        "User id not found"
                ));

        users.delete(user);
    }

    private UserEntity requireCurrentWithPassword(String phoneFromAuth, DeleteAccountReq req) {
        if (req == null || req.password() == null || req.password().isBlank()
                || req.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_INVALID",
                    "Password is invalid"
            );
        }

        UserEntity user = findCurrent(phoneFromAuth);

        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "CREDENTIALS_INVALID",
                    "Credentials are invalid"
            );
        }

        return user;
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
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "USER_PHONE_NOT_FOUND",
                    "User with given phone not found"
            );
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);

        return users.findByPhoneHash(phoneHash)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_PHONE_NOT_FOUND",
                        "User with given phone not found"
                ));
    }

    private UserEntity findCurrentForUpdate(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);

        if (phoneNorm.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "USER_PHONE_NOT_FOUND",
                    "User with given phone not found"
            );
        }

        return users.findByPhoneHashForUpdate(Crypto.sha256Hex(phoneNorm))
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_PHONE_NOT_FOUND",
                        "User with given phone not found"
                ));
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ID_INVALID",
                    "Id is invalid"
            );
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String s = value.trim();
        return s.isEmpty() ? null : s;
    }
}