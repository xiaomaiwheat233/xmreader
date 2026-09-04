package com.xmreader.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    private static final String USERNAME = "wheat_reader";
    private static final String PASSWORD = "reader-pass-123";
    private static final String NEW_PASSWORD = "reader-pass-456";
    private static final String RECOVERED_PASSWORD = "reader-pass-789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM user_sessions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registrationLoginRefreshProfilePasswordAndLogoutFormAClosedLoop() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"WHEAT_READER","password":"reader-pass-123","confirmPassword":"reader-pass-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(USERNAME))
                .andReturn();
        assertThat(readJson(registration).path("data").path("id").asText()).isNotBlank();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wheat_reader","password":"reader-pass-123","confirmPassword":"reader-pass-123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.type").value("USERNAME_ALREADY_EXISTS"));

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?", String.class, USERNAME);
        assertThat(passwordHash).startsWith("$2").doesNotContain(PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wheat_reader","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.type").value("INVALID_CREDENTIALS"));

        MvcResult login = login(PASSWORD);
        JsonNode loginBody = readJson(login);
        String accessToken = loginBody.path("data").path("accessToken").asText();
        String firstRefreshToken = refreshToken(login);
        assertThat(accessToken).isNotBlank();
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth");

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.type").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"麦田书友","avatarUrl":"https://images.example.test/avatar.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("麦田书友"));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avatarUrl":"http://localhost/avatar.png"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("INVALID_AVATAR_URL"));

        mockMvc.perform(get("/api/admin/not-yet-implemented")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.type").value("FORBIDDEN"));

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();
        String rotatedRefreshToken = refreshToken(refresh);
        String refreshedAccessToken = readJson(refresh).path("data").path("accessToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.type").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"reader-pass-123","newPassword":"reader-pass-456"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
        loginExpectingUnauthorized(PASSWORD);

        MvcResult secondLogin = login(NEW_PASSWORD);
        String finalRefreshToken = refreshToken(secondLogin);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wheat_reader","newPassword":"reader-pass-789","confirmPassword":"reader-pass-789"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(finalRefreshToken)))
                .andExpect(status().isUnauthorized());
        loginExpectingUnauthorized(NEW_PASSWORD);

        MvcResult recoveredLogin = login(RECOVERED_PASSWORD);
        String recoveredRefreshToken = refreshToken(recoveredLogin);
        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie(recoveredRefreshToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(recoveredRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordConfirmationAndUnknownRecoveryAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new_reader","password":"reader-pass-123","confirmPassword":"reader-pass-456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing_reader","newPassword":"reader-pass-789","confirmPassword":"reader-pass-789"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.type").value("USER_NOT_FOUND"));
    }

    private MvcResult login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();
    }

    private void loginExpectingUnauthorized(String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String refreshToken(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).isNotNull();
        String prefix = "xmreader_refresh=";
        int start = header.indexOf(prefix) + prefix.length();
        return header.substring(start, header.indexOf(';', start));
    }

    private Cookie refreshCookie(String value) {
        return new Cookie("xmreader_refresh", value);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
