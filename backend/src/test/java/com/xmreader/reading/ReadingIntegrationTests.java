package com.xmreader.reading;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class ReadingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsers() {
        jdbcTemplate.update("DELETE FROM user_sessions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void bookshelfProgressAndHistoryFormAnAuthenticatedClosedLoop() throws Exception {
        String token = registerAndLogin();
        String firstBookId = unsignedId("SELECT id FROM books ORDER BY id LIMIT 1");
        String firstChapterId = unsignedId(
                "SELECT id FROM chapters WHERE book_id = ? ORDER BY chapter_index LIMIT 1", firstBookId);
        String otherChapterId = unsignedId(
                "SELECT c.id FROM chapters c WHERE c.book_id <> ? ORDER BY c.id LIMIT 1", firstBookId);

        mockMvc.perform(get("/api/bookshelf"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reading-progress/{bookId}", firstBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        addToBookshelf(token, firstBookId);
        addToBookshelf(token, firstBookId);
        mockMvc.perform(get("/api/bookshelf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].book.id").value(firstBookId))
                .andExpect(jsonPath("$.data.items[0].progress").doesNotExist());

        mockMvc.perform(get("/api/books/{bookId}", firstBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inBookshelf").value(true));

        mockMvc.perform(put("/api/reading-progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId":"%s","chapterId":"%s","position":10,"progressPercent":42.50}
                                """.formatted(firstBookId, firstChapterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapterId").value(firstChapterId))
                .andExpect(jsonPath("$.data.position").value(10))
                .andExpect(jsonPath("$.data.progressPercent").value(42.5));

        mockMvc.perform(get("/api/reading-progress/{bookId}", firstBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapterTitle").isNotEmpty());

        mockMvc.perform(get("/api/reading-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].chapter.id").value(firstChapterId));

        mockMvc.perform(put("/api/reading-progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId":"%s","chapterId":"%s","position":0,"progressPercent":0}
                                """.formatted(firstBookId, otherChapterId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("INVALID_QUERY"));

        removeHistory(token, firstBookId);
        removeHistory(token, firstBookId);
        removeFromBookshelf(token, firstBookId);
        removeFromBookshelf(token, firstBookId);

        mockMvc.perform(get("/api/bookshelf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/reading-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"library_reader","password":"reader-pass-123","nickname":"书架测试读者"}
                                """))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"library_reader","password":"reader-pass-123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data").path("accessToken").asText();
    }

    private void addToBookshelf(String token, String bookId) throws Exception {
        mockMvc.perform(put("/api/bookshelf/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private void removeFromBookshelf(String token, String bookId) throws Exception {
        mockMvc.perform(delete("/api/bookshelf/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private void removeHistory(String token, String bookId) throws Exception {
        mockMvc.perform(delete("/api/reading-history/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private String unsignedId(String sql, Object... arguments) {
        Long id = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return Long.toUnsignedString(id);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
