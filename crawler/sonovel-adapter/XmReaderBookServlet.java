package com.pcdd.sonovel.web.servlet;

import cn.hutool.core.bean.BeanUtil;
import com.pcdd.sonovel.context.BookContext;
import com.pcdd.sonovel.core.AppConfigLoader;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Chapter;
import com.pcdd.sonovel.model.Rule;
import com.pcdd.sonovel.parser.BookParser;
import com.pcdd.sonovel.parser.ChapterParser;
import com.pcdd.sonovel.parser.TocParser;
import com.pcdd.sonovel.utils.SourceUtils;
import com.pcdd.sonovel.utils.VirtualThreadLimiter;
import com.pcdd.sonovel.web.util.RespUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structured endpoint maintained by xmreader on top of a pinned so-novel
 * version. It imports every chapter exposed by the selected source's catalog.
 */
public class XmReaderBookServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String bookUrl = request.getParameter("url");
            Rule rule = validateSourceUrl(bookUrl);

            AppConfig config = BeanUtil.copyProperties(AppConfigLoader.APP_CONFIG, AppConfig.class);
            config.setSourceId(rule.getId());
            config.setExtName("txt");
            config.setEnableProgressbar(0);

            Rule.Book book = new BookParser(config).parse(bookUrl);
            BookContext.set(book);
            try {
                List<Chapter> toc = new TocParser(config).parseAll(bookUrl);
                if (toc.isEmpty()) {
                    RespUtils.writeError(response, 500, "Source chapter list is empty");
                    return;
                }

                ChapterParser chapterParser = new ChapterParser(config);
                int maxConcurrent = config.getConcurrency() == -1
                        ? Math.min(4, toc.size())
                        : Math.max(1, Math.min(config.getConcurrency(), Math.min(4, toc.size())));
                Map<Integer, Chapter> parsedChapters = new ConcurrentHashMap<>();
                try (var limiter = new VirtualThreadLimiter(maxConcurrent)) {
                    for (int index = 0; index < toc.size(); index++) {
                        int chapterIndex = index;
                        limiter.submit(() -> {
                            Chapter parsed = chapterParser.parse(toc.get(chapterIndex));
                            if (parsed != null && parsed.getContent() != null && !parsed.getContent().isBlank()) {
                                parsedChapters.put(chapterIndex, parsed);
                            }
                        });
                    }
                }

                int normalizedChapterIndex = 1;
                List<Map<String, Object>> chapters = new ArrayList<>();
                for (int index = 0; index < toc.size(); index++) {
                    Chapter chapter = parsedChapters.get(index);
                    if (chapter == null) continue;
                    Map<String, Object> chapterPayload = new LinkedHashMap<>();
                    chapterPayload.put("chapterIndex", normalizedChapterIndex++);
                    chapterPayload.put("title", chapter.getTitle());
                    chapterPayload.put("sourceUrl", chapter.getUrl());
                    chapterPayload.put("content", removeRenderedTitle(chapter));
                    chapters.add(chapterPayload);
                }
                if (chapters.isEmpty()) {
                    RespUtils.writeError(response, 500, "Source chapter content is empty");
                    return;
                }

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("sourceId", rule.getId());
                payload.put("sourceName", rule.getName());
                payload.put("sourceBaseUrl", rule.getUrl());
                payload.put("sourceUrl", bookUrl);
                payload.put("title", book.getBookName());
                payload.put("author", book.getAuthor());
                payload.put("description", book.getIntro());
                payload.put("coverUrl", book.getCoverUrl());
                payload.put("category", book.getCategory());
                payload.put("status", book.getStatus());
                payload.put("latestChapterTitle", book.getLatestChapter());
                payload.put("chapters", chapters);
                RespUtils.writeJson(response, payload);
            } finally {
                BookContext.clear();
            }
        } catch (IllegalArgumentException exception) {
            RespUtils.writeError(response, 400, exception.getMessage());
        } catch (Exception exception) {
            RespUtils.writeError(response, 500, "Crawler adapter failed: " + exception.getMessage());
        }
    }

    private Rule validateSourceUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > 2048) {
            throw new IllegalArgumentException("A valid source URL is required");
        }

        URI requested = URI.create(rawUrl);
        if (!("http".equalsIgnoreCase(requested.getScheme()) || "https".equalsIgnoreCase(requested.getScheme()))
                || requested.getHost() == null
                || requested.getUserInfo() != null) {
            throw new IllegalArgumentException("Only HTTP(S) source URLs without user info are allowed");
        }

        Rule rule = SourceUtils.getRule(rawUrl);
        URI allowed = URI.create(rule.getUrl());
        if (rule.isDisabled()
                || allowed.getHost() == null
                || !allowed.getHost().equalsIgnoreCase(requested.getHost())
                || normalizedPort(allowed) != normalizedPort(requested)) {
            throw new IllegalArgumentException("The URL does not match an enabled source host");
        }
        return rule;
    }

    private int normalizedPort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String removeRenderedTitle(Chapter chapter) {
        String content = chapter.getContent().replace("\r\n", "\n").trim();
        String prefix = chapter.getTitle() + "\n\n";
        return content.startsWith(prefix) ? content.substring(prefix.length()).trim() : content;
    }
}
