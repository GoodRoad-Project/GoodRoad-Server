package goodroad.security;

import goodroad.tokens.RefreshTokenService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepo users;
    private final RefreshTokenService refreshTokenService;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepo users, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.users = users;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        try {
            if (!jwtService.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtService.parseClaims(token);
            String phoneNorm = Crypto.normPhone(claims.getSubject());
            Long tokenUserId = jwtService.getUserIdFromToken(token);
            Integer tokenVersion = jwtService.getTokenVersionFromToken(token);

            if (!phoneNorm.isEmpty()) {
                UserEntity user = users.findByPhoneHash(Crypto.sha256Hex(phoneNorm)).orElse(null);

                if (user != null && user.isActive()) {
                    if (user.getTokenVersion() != null && tokenVersion != null && !user.getTokenVersion().equals(tokenVersion)) {
                        refreshTokenService.revokeAllUserTokens(user.getId());
                        response.setStatus(401);
                        response.getWriter().write("Token version mismatch, please login again");
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            phoneNorm,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}