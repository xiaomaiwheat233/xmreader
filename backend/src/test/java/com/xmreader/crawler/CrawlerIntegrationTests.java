package com.xmreader.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xmreader.catalog.BookEntity;
import com.xmreader.catalog.BookMapper;
import com.xmreader.catalog.ChapterEntity;
import com.xmreader.catalog.ChapterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrawlerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @MockBean
    private CrawlerGateway crawlerGateway;

    @Test
    void onlineSearchIsPublicAndUsesNormalizedAdapterResults() throws Exception {
        when(crawlerGateway.search("测试小说")).thenReturn(List.of(new OnlineBookCandidate(
                7,
                "授权测试源",
                "https://books.example.test/book/42",
                "测试小说",
                "测试作者",
                "测试简介",
                "文学",
                "第二章",
                "2026-09-04",
                "连载中",
                "1000",
                true)));

        mockMvc.perform(get("/api/crawler/search").param("q", " 测试小说 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceName").value("授权测试源"))
                .andExpect(jsonPath("$.data[0].title").value("测试小说"));
    }

    @Test
    void importedPreviewIsPersistedInTheExistingCatalog() {
        String sourceUrl = "https://books.example.test/book/42";
        when(crawlerGateway.fetchBook(sourceUrl)).thenReturn(new CrawledBook(
                7,
                "授权测试源",
                "https://books.example.test/",
                sourceUrl,
                "联网测试小说",
                "测试作者",
                "只用于自动化测试",
                null,
                "文学",
                "连载中",
                "第二章",
                List.of(
                        new CrawledBook.CrawledChapter(
                                1, "第一章", sourceUrl + "/1", "第一段。\n\n第二段。"),
                        new CrawledBook.CrawledChapter(
                                2, "第二章", sourceUrl + "/2", "新的正文。"))));

        ImportedBookResponse imported = crawlerService.importBook(sourceUrl);

        BookEntity book = bookMapper.selectById(Long.parseUnsignedLong(imported.bookId()));
        List<ChapterEntity> chapters = chapterMapper.selectList(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, book.getId())
                .orderByAsc(ChapterEntity::getChapterIndex));
        assertThat(book.getTitle()).isEqualTo("联网测试小说");
        assertThat(book.getStatus()).isEqualTo("ONGOING");
        assertThat(book.getChapterCount()).isEqualTo(2);
        assertThat(chapters).extracting(ChapterEntity::getTitle).containsExactly("第一章", "第二章");
        assertThat(chapters.get(0).getContent()).doesNotContain("<p>");
    }

    @Test
    void importEndpointRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/crawler/imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
