package com.xmreader.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successCreatesSuccessfulResponseWithData() {
        ApiResponse<String> response = ApiResponse.success("payload", "request-1");

        assertThat(response.code()).isZero();
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.error()).isNull();
        assertThat(response.requestId()).isEqualTo("request-1");
    }

    @Test
    void failureCreatesErrorResponseWithoutData() {
        ApiError error = new ApiError("validation", Map.of("username", "required"));

        ApiResponse<Void> response = ApiResponse.failure(400, "invalid request", error, "request-2");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("invalid request");
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.requestId()).isEqualTo("request-2");
    }
}
