package com.novelhub.shared.web;

public record ApiResponse<T>(int code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "success", data, requestId);
    }
}
