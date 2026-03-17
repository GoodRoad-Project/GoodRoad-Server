package goodroad.users.moderators;

import goodroad.api.ApiErrors.ApiException;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.model.Role;
import goodroad.security.Crypto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModeratorService {

    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;

    public ModeratorService(UserRepo users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public record createReq(
            String firstName,
            String lastName,
            String phone,
            String password
    ) {}

    @Transactional
    public String create(String firstName, String lastName, String phone, String password) {
        String phoneNorm = Crypto.normPhone(phone);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_INVALID", "Phone number is invalid");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "Phone number already used");
        }

        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phoneHash(phoneHash)
                .role(Role.MODERATOR.name())
                .passHash(passwordEncoder.encode(password))
                .active(true)
                .build();

        return users.save(user).getId().toString();
    }

    @Transactional
    public void disable(String id) {
        Long userId = parseId(id);
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_ID_NOT_FOUND", "User id not found"));

        if (Role.MODERATOR_ADMIN.name().equals(user.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_ADMIN_CANNOT_BE_DISABLED", "Cant disable admin moderator");
        }

        user.setActive(false);
        users.save(user);
    }

    public record ModeratorView(
            String id,
            String role,
            String firstName,
            String lastName,
            String photoUrl,
            boolean active
    ) {
    }

    @Transactional(readOnly = true)
    public List<ModeratorView> getAllModerators() {
        return users.findByRoleIn(List.of(Role.MODERATOR.name(), Role.MODERATOR_ADMIN.name())).stream()
                .map(this::toView)
                .toList();
    }

    private ModeratorView toView(UserEntity user) {
        return new ModeratorView(
                user.getId().toString(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhotoUrl(),
                user.isActive()
        );
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Id is invalid");
        }
    }

}