package com.xmreader.auth;

import java.time.Duration;

public record AuthResult(AuthResponse response, String refreshToken, Duration refreshTokenTtl) {
}
