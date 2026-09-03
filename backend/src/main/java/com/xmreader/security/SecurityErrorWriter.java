package com.xmreader.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmreader.shared.web.ApiError;
import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, 401, 40102, "UNAUTHORIZED", "请先登录或重新登录");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, 403, 40301, "FORBIDDEN", "没有权限执行此操作");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int httpStatus,
            int code,
            String type,
            String message) throws IOException {
        Object requestIdValue = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        String requestId = requestIdValue instanceof String id ? id : "unknown";
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(code, message, ApiError.of(type), requestId));
    }
}
