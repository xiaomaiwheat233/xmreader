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
import com.pcdd.sonovel.web.util.RespUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal structured endpoint maintained by xmreader on top of a pinned
 * so-novel version. It intentionally limits each request to a small preview.
 */
public class XmReaderBookServlet extends HttpServlet {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String bookUrl = request.getParameter("url");
            int limit = parseLimit(request.getParameter("limit"));
            Rule rule = validateSourceUrl(bookUrl);

            AppConfig config = BeanUtil.copyProperties(AppConfigLoader.APP_CONFIG, AppConfig.class);
            config.setSourceId(rule.getId());
            config.setExtName("txt");
            config.setEnableProgressbar(0);
            config.setConcurrency(1);

            Rule.Book book = new BookParser(config).parse(bookUrl);
            BookContext.set(book);
            try {
                List<Chapter> toc = new TocParser(config).parse(bookUrl, 1, limit)
                        .stream()
                        .limit(limit)
                        .toList();
                if (toc.isEmpty()) {
                    RespUtils.writeError(response, 500, "Source chapter list is empty");
                    return;
                }

                ChapterParser chapterParser = new ChapterParser(config);
                int normalizedChapterIndex = 1;
                List<Map<String, Object>> chapters = new ArrayList<>();
                for (Chapter chapterRef : toc) {
                    Chapter chapter = chapterParser.parse(chapterRef);
                    if (chapter == null || chapter.getContent() == null || chapter.getContent().isBlank()) {
                        continue;
                    }
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

    private int parseLimit(String rawLimit) {
        if (rawLimit == null || rawLimit.isBlank()) {
            return DEFAULT_LIMIT;
        }
        try {
            int limit = Integer.parseInt(rawLimit);
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("limit must be an integer", exception);
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
