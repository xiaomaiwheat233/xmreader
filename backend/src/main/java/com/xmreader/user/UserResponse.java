package com.xmreader.user;

public record UserResponse(
        String id,
        String username,
        String nickname,
        String avatarUrl,
        String role) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                Long.toUnsignedString(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole());
    }
}
