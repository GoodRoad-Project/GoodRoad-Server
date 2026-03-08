package GoodRoad.security;

import GoodRoad.database.UserEntity;
import GoodRoad.database.UserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        //.requestMatchers("/users/moderators/{id}").hasAnyRole("MODERATOR", "MODERATOR_ADMIN") // TODO: возможно пересмотреть права пользователей на этот ендпоинт
                        .requestMatchers("/users/moderators/**").hasRole("MODERATOR_ADMIN")
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepo users) {
        return username -> {
            String phoneNorm = Crypto.normPhone(username);
            String phoneHash = Crypto.sha256Hex(phoneNorm);

            UserEntity u = users.findByPhoneHash(phoneHash)
                    .orElseThrow(() -> new UsernameNotFoundException("No user"));

            if (!u.isActive()) {
                throw new UsernameNotFoundException("Inactive user");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(phoneNorm)
                    .password(u.getPassHash())
                    .authorities("ROLE_" + u.getRole())
                    .build();
        };
    }
}