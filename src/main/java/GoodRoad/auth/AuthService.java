package GoodRoad.auth;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.database.UserEntity;
import GoodRoad.database.UserRepo;
import GoodRoad.security.Crypto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepo users;
    private final PasswordEncoder pe;

    public AuthService(UserRepo users, PasswordEncoder pe) {
        this.users = users;
        this.pe = pe;
    }

    @Transactional
    public AuthController.AuthResp register(AuthController.RegisterReq req) {
        String phoneNorm = Crypto.normPhone(req.phone()); // нормализуем телефон, оставляем только цифры
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PHONE", "Bad phone");
        }

        String phoneHash = Crypto.sha256Hex(phoneNorm);
        if (users.findByPhoneHash(phoneHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_USED", "Phone already used");
        }

        Instant now = Instant.now();

        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setPhoneHash(phoneHash);
        u.setRole("USER");
        u.setPassHash(pe.encode(req.password()));
        u.setActive(true);
        u.setCreatedAt(now);
        u.setLastActiveAt(now);

        users.save(u);
        return toResp(u);
    }

    @Transactional
    public AuthController.AuthResp login(AuthController.LoginReq req) {
        String phoneNorm = Crypto.normPhone(req.phone());
        String phoneHash = Crypto.sha256Hex(phoneNorm);

        UserEntity u = users.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDS", "Bad credentials"));

        if (!u.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INACTIVE", "Account inactive");
        }
        if (!pe.matches(req.password(), u.getPassHash())) { // проверяем пароль
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDS", "Bad credentials");
        }

        u.setLastActiveAt(Instant.now()); // в таблицах аккаунт автоудаляется через 5 лет, поэтому проверяем последнюю активность
        users.save(u);

        return toResp(u);
    }

    @Transactional
    public void changePass(String phoneFromAuth, AuthController.ChangePassReq req) {
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
        if (!pe.matches(req.oldPassword(), u.getPassHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_OLD_PASS", "Bad old password");
        }

        u.setPassHash(pe.encode(req.newPassword())); // хэш нового пароля
        u.setLastActiveAt(Instant.now());
        users.save(u);
    }

    private AuthController.AuthResp toResp(UserEntity u) {
        return new AuthController.AuthResp(new AuthController.UserView(u.getId().toString(), u.getRole()));
    }
}