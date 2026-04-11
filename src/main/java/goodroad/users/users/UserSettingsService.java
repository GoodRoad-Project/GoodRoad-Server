package goodroad.users.users;

import goodroad.api.ApiErrors.ApiException;
import goodroad.auth.AuthService;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
@Service
public class UserSettingsService {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final Path avatarDir;

    public UserSettingsService(
            UserRepo users,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            @Value("${app.avatar.dir:uploads/avatars}") String avatarDir
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.avatarDir = Paths.get(avatarDir).toAbsolutePath().normalize();
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

    public record AvatarUploadResp(
            String photoUrl
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
    public AvatarUploadResp uploadAvatar(String phoneFromAuth, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_EMPTY", "Avatar file is empty");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_TOO_LARGE", "Avatar file is too large");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_TYPE_INVALID", "Avatar file type is invalid");
        }

        UserEntity user = findCurrent(phoneFromAuth);
        String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
        String fileName = user.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(avatarDir);
            Path target = avatarDir.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/users/avatar/")
                    .path(fileName)
                    .toUriString();

            return new AvatarUploadResp(photoUrl);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_SAVE_FAILED", "Avatar file could not be saved");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getAvatar(String fileName) {
        try {
            Path file = avatarDir.resolve(fileName).normalize();
            if (!file.startsWith(avatarDir) || !Files.exists(file) || !Files.isRegularFile(file)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "AVATAR_NOT_FOUND", "Avatar file not found");
            }

            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "AVATAR_NOT_FOUND", "Avatar file not found");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_READ_FAILED", "Avatar file could not be read");
        }
    }

    @Transactional
    public void deleteCurrent(String phoneFromAuth, DeleteAccountReq req) {
        UserEntity user = requireCurrentWithPassword(phoneFromAuth, req);
        if (!Role.USER.name().equals(user.getRole())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "USER_CANT_DELETE",
                    "Only regular users can delete their account"
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_ID_NOT_FOUND", "User id not found"));

        users.delete(user);
    }

    private UserEntity requireCurrentWithPassword(String phoneFromAuth, DeleteAccountReq req) {
        if (req == null || req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_INVALID", "Password is invalid");
        }

        UserEntity user= findCurrent(phoneFromAuth);
        if (!passwordEncoder.matches(req.password(), user.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENTIALS_INVALID", "Credentials are invalid");
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
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        return users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Id is invalid");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }

    private static String resolveExtension(String contentType, String originalFilename) {
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        return extension == null || extension.isBlank() ? "" : "." + extension;
    }
}