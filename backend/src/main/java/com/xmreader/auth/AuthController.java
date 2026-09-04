package com.xmreader.auth;

import com.xmreader.security.SecurityProperties;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.RequestIdFilter;
import com.xmreader.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties properties;

    public AuthController(AuthService authService, SecurityProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request), requestId(servletRequest)));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest) {
        authService.resetPassword(request);
        return ApiResponse.success(null, requestId(servletRequest));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthResult result = authService.login(request);
        setRefreshCookie(servletResponse, result);
        return ApiResponse.success(result.response(), requestId(servletRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
            AuthResult result = authService.refresh(refreshToken);
            setRefreshCookie(servletResponse, result);
            return ApiResponse.success(result.response(), requestId(servletRequest));
        } catch (BusinessException exception) {
            clearRefreshCookie(servletResponse);
            throw exception;
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        authService.logout(refreshToken);
        clearRefreshCookie(servletResponse);
        return ApiResponse.success(null, requestId(servletRequest));
    }

    private void setRefreshCookie(HttpServletResponse response, AuthResult result) {
        ResponseCookie cookie = baseCookie(result.refreshToken())
                .maxAge(result.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/auth");
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
