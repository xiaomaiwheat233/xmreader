package com.xmreader.user;

import com.xmreader.auth.UserSessionMapper;
import com.xmreader.shared.exception.BusinessException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(
            UserMapper userMapper,
            UserSessionMapper sessionMapper,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public UserResponse getCurrent(long userId) {
        return UserResponse.from(requireActiveUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(long userId, UpdateProfileRequest request) {
        if (request.nickname() == null && request.avatarUrl() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, 40003, "EMPTY_UPDATE", "至少需要提供一个可修改字段");
        }
        UserEntity user = requireActiveUser(userId);
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, 40001, "VALIDATION_ERROR", "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(validateAvatarUrl(request.avatarUrl()));
        }
        user.setUpdatedAt(now());
        userMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(long userId, ChangePasswordRequest request) {
        UserEntity user = requireActiveUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, 40004, "CURRENT_PASSWORD_INVALID", "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, 40005, "PASSWORD_UNCHANGED", "新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(now());
        userMapper.updateById(user);
        sessionMapper.revokeAllForUser(userId, now());
    }

    private UserEntity requireActiveUser(long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 40401, "USER_NOT_FOUND", "用户不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 40302, "USER_DISABLED", "账号已被禁用");
        }
        return user;
    }

    private String validateAvatarUrl(String rawValue) {
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw invalidAvatarUrl();
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw invalidAvatarUrl();
        }
    }

    private BusinessException invalidAvatarUrl() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST, 40006, "INVALID_AVATAR_URL", "头像地址必须是有效的 HTTPS URL");
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
