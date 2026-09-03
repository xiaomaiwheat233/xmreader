package com.xmreader.shared.web;

public record ApiResponse<T>(int code, String message, T data, ApiError error, String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "success", data, null, requestId);
    }

    public static ApiResponse<Void> failure(int code, String message, ApiError error, String requestId) {
        return new ApiResponse<>(code, message, null, error, requestId);
    }
}
