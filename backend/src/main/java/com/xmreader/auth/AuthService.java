package com.xmreader.auth;

import com.xmreader.security.SecurityProperties;
import com.xmreader.security.TokenService;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.user.UserEntity;
import com.xmreader.user.UserMapper;
import com.xmreader.user.UserResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final UserSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecurityProperties properties;
    private final Clock clock;

    public AuthService(
            UserMapper userMapper,
            UserSessionMapper sessionMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            SecurityProperties properties,
            Clock clock) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userMapper.findByUsername(username) != null) {
            throw usernameExists();
        }

        LocalDateTime now = now();
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw usernameExists();
        }
        return UserResponse.from(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(normalizeUsername(request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 40302, "USER_DISABLED", "账号已被禁用");
        }

        String refreshToken = newRefreshToken();
        LocalDateTime now = now();
        UserSessionEntity session = new UserSessionEntity();
        session.setUserId(user.getId());
        session.setRefreshTokenHash(hash(refreshToken));
        session.setExpiresAt(now.plus(properties.refreshTokenTtl()));
        session.setCreatedAt(now);
        sessionMapper.insert(session);
        return authResult(user, session.getId(), refreshToken);
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        byte[] oldHash = hash(refreshToken);
        UserSessionEntity session = sessionMapper.findByRefreshTokenHash(oldHash);
        LocalDateTime now = now();
        if (session == null || session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)) {
            throw invalidRefreshToken();
        }
        UserEntity user = userMapper.selectById(session.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            sessionMapper.revokeByRefreshTokenHash(oldHash, now);
            throw invalidRefreshToken();
        }

        String newRefreshToken = newRefreshToken();
        if (sessionMapper.rotate(session.getId(), oldHash, hash(newRefreshToken), now) != 1) {
            throw invalidRefreshToken();
        }
        return authResult(user, session.getId(), newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionMapper.revokeByRefreshTokenHash(hash(refreshToken), now());
        }
    }

    private AuthResult authResult(UserEntity user, long sessionId, String refreshToken) {
        AuthResponse response = new AuthResponse(
                tokenService.createAccessToken(user, sessionId),
                "Bearer",
                tokenService.accessTokenTtlSeconds(),
                UserResponse.from(user));
        return new AuthResult(response, refreshToken, properties.refreshTokenTtl());
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private BusinessException usernameExists() {
        return new BusinessException(HttpStatus.CONFLICT, 40901, "USERNAME_ALREADY_EXISTS", "用户名已存在");
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, 40101, "INVALID_CREDENTIALS", "用户名或密码错误");
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, 40103, "INVALID_REFRESH_TOKEN", "登录状态已失效");
    }
}
