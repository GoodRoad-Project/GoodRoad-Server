package GoodRoad.users.moderators;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import GoodRoad.model.Role;
import GoodRoad.security.Crypto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PHONE", "Bad phone");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_USED", "Phone already used");
        }

        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setPhoneHash(phoneHash);
        u.setRole(Role.MODERATOR.name());
        u.setPassHash(passwordEncoder.encode(password));
        u.setActive(true);

        return users.save(u).getId().toString();
    }

    @Transactional
    public void disable(String id) {
        Long userId = parseId(id);
        UserEntity u = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_USER", "No user"));

        if (Role.MODERATOR_ADMIN.name().equals(u.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANT_DISABLE_ADMIN", "Cant disable admin moderator");
        }

        u.setActive(false);
        users.save(u);
    }

    @Transactional
    public List<UserEntity> getAllModerators() {
        return users.findAll().stream()
                .filter(u -> Role.MODERATOR.name().equals(u.getRole()) || Role.MODERATOR_ADMIN.name().equals(u.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteModerator(String id) {
        Long userId = parseId(id);
        UserEntity u = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_USER", "No user"));

        if (Role.MODERATOR_ADMIN.name().equals(u.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANT_DELETE_ADMIN", "Cant delete admin moderator");
        }

        if (!Role.MODERATOR.name().equals(u.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_MODERATOR", "User is not a moderator");
        }

        users.delete(u);
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_ID", "Bad id");
        }
    }

}
