package com.xmreader.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 64, message = "昵称长度必须为 1 到 64") String nickname,
        @Size(max = 2048, message = "头像地址不能超过 2048 个字符") String avatarUrl) {
}
