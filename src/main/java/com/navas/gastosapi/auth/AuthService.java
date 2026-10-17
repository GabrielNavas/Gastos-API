package com.navas.gastosapi.auth;

import com.navas.gastosapi.common.ConflictException;
import com.navas.gastosapi.common.UnauthorizedException;
import com.navas.gastosapi.config.JwtProperties;
import com.navas.gastosapi.user.User;
import com.navas.gastosapi.user.UserRepository;
import com.navas.gastosapi.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final JwtProperties properties;
    // Usado para gastar o mesmo tempo de BCrypt quando o e-mail nao existe
    private final String dummyHash;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder,
                       JwtService jwtService,
                       JwtProperties properties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.dummyHash = encoder.encode("senha-dummy-para-igualar-tempo");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }
        User user = users.save(new User(email, encoder.encode(request.password())));
        return new UserResponse(user.getId(), user.getEmail());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmail(normalize(request.email()));
        String hashToCompare = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = encoder.matches(request.password(), hashToCompare);
        if (user.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        return issueTokens(user.get());
    }

    /**
     * Rotaciona o refresh token: o antigo e revogado e um novo par e emitido.
     * Se um token ja revogado for reapresentado (possivel roubo), todas as
     * sessoes do usuario sao revogadas. O noRollbackFor garante que essa
     * revogacao seja gravada mesmo com a excecao 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(request.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (stored.isRevoked()) {
            refreshTokens.revokeAllByUserId(stored.getUserId());
            throw new UnauthorizedException("Refresh token inválido");
        }
        if (stored.getExpiresAt().isBefore(now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));
        stored.revoke();
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.findByTokenHash(sha256(request.refreshToken()))
                .ifPresent(RefreshToken::revoke);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateRefreshToken();
        refreshTokens.save(new RefreshToken(
                user.getId(),
                sha256(rawRefreshToken),
                now().plusDays(properties.refreshTtlDays())));
        return new TokenResponse(accessToken, rawRefreshToken, "Bearer", jwtService.accessTtlSeconds());
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
