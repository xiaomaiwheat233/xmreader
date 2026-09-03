package com.xmreader.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void homeSearchDetailDirectoryAndReaderApisReturnOriginalFixtures() throws Exception {
        MvcResult homeResult = mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommended.length()").value(3))
                .andExpect(jsonPath("$.data.recentlyUpdated.length()").value(3))
                .andExpect(jsonPath("$.data.popular.length()").value(3))
                .andReturn();
        JsonNode firstBook = json(homeResult).path("data").path("recommended").get(0);
        String bookId = firstBook.path("id").asText();

        mockMvc.perform(get("/api/search").param("q", "麦田"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("麦田来信"));

        mockMvc.perform(get("/api/search").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/books")
                        .param("page", "1")
                        .param("pageSize", "2")
                        .param("sort", "POPULAR_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source.name").value("小麦原创测试书库"))
                .andExpect(jsonPath("$.data.chapterCount").value(3));

        MvcResult chaptersResult = mockMvc.perform(get("/api/books/{id}/chapters", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].chapterIndex").value(1))
                .andReturn();
        String firstChapterId = json(chaptersResult).path("data").path("items").get(0).path("id").asText();

        MvcResult chapterResult = mockMvc.perform(get("/api/chapters/{id}", firstChapterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousChapterId").doesNotExist())
                .andExpect(jsonPath("$.data.nextChapterId").isNotEmpty())
                .andReturn();
        String content = json(chapterResult).path("data").path("content").asText();
        assertThat(content).isNotBlank().doesNotContain("<script", "<p>");

        mockMvc.perform(get("/api/books/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("INVALID_QUERY"));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
