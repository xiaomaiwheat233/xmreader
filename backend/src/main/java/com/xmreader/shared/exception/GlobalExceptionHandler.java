package com.xmreader.shared.exception;

import com.xmreader.shared.web.ApiError;
import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException exception,
            HttpServletRequest request) {
        return failure(
                exception.getStatus(),
                exception.getCode(),
                exception.getType(),
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return failure(
                HttpStatus.BAD_REQUEST,
                40001,
                "VALIDATION_ERROR",
                "请求参数校验失败",
                fields,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return failure(
                HttpStatus.BAD_REQUEST,
                40002,
                "INVALID_JSON",
                "请求内容格式不正确",
                Map.of(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String requestId = requestId(request);
        log.error("Unhandled request error, requestId={}", requestId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        50000,
                        "服务器内部错误",
                        ApiError.of("INTERNAL_ERROR"),
                        requestId));
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            HttpStatus status,
            int code,
            String type,
            String message,
            Map<String, String> fields,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, new ApiError(type, fields), requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return value instanceof String id ? id : "unknown";
    }
}
