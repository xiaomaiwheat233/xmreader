package com.xmreader.crawler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xmreader.catalog.BookEntity;
import com.xmreader.catalog.BookMapper;
import com.xmreader.catalog.ChapterEntity;
import com.xmreader.catalog.ChapterMapper;
import com.xmreader.catalog.ContentSourceEntity;
import com.xmreader.catalog.ContentSourceMapper;
import com.xmreader.shared.exception.BusinessException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrawlerCatalogWriter {

    private final ContentSourceMapper sourceMapper;
    private final BookMapper bookMapper;
    private final ChapterMapper chapterMapper;
    private final Clock clock;

    public CrawlerCatalogWriter(
            ContentSourceMapper sourceMapper,
            BookMapper bookMapper,
            ChapterMapper chapterMapper,
            Clock clock) {
        this.sourceMapper = sourceMapper;
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.clock = clock;
    }

    @Transactional
    public ImportedBookResponse save(CrawledBook crawledBook) {
        validate(crawledBook);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        String sourceUrl = normalizeUrl(crawledBook.sourceUrl());
        String sourceBaseUrl = normalizeBaseUrl(crawledBook.sourceBaseUrl(), sourceUrl);
        ContentSourceEntity source = findOrCreateSource(crawledBook, sourceBaseUrl, now);

        byte[] sourceUrlHash = hash(sourceUrl);
        BookEntity book = bookMapper.selectOne(new LambdaQueryWrapper<BookEntity>()
                .eq(BookEntity::getSourceId, source.getId())
                .eq(BookEntity::getSourceUrlHash, sourceUrlHash)
                .last("LIMIT 1"));
        if (book == null) {
            book = new BookEntity();
            book.setSourceId(source.getId());
            book.setSourceBookId(HexFormat.of().formatHex(sourceUrlHash));
            book.setSourceUrl(sourceUrl);
            book.setSourceUrlHash(sourceUrlHash);
            book.setVisibility("VISIBLE");
            book.setWordCount(0L);
            book.setChapterCount(0);
            book.setCreatedAt(now);
        }

        book.setTitle(truncate(crawledBook.title(), 255));
        book.setAuthor(truncate(value(crawledBook.author(), "未知作者"), 128));
        book.setDescription(truncate(crawledBook.description(), 16_000));
        book.setCoverUrl(truncate(crawledBook.coverUrl(), 2048));
        book.setCategory(truncate(crawledBook.category(), 64));
        book.setStatus(normalizeStatus(crawledBook.status()));
        book.setLastCrawledAt(now);
        book.setDeletedAt(null);
        book.setUpdatedAt(now);
        if (book.getId() == null) {
            bookMapper.insert(book);
        } else {
            bookMapper.updateById(book);
        }

        for (CrawledBook.CrawledChapter crawledChapter : crawledBook.chapters()) {
            saveChapter(book.getId(), crawledChapter, now);
        }

        List<ChapterEntity> chapters = chapterMapper.selectList(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, book.getId())
                .orderByAsc(ChapterEntity::getChapterIndex));
        ChapterEntity latest = chapters.isEmpty() ? null : chapters.get(chapters.size() - 1);
        long wordCount = chapters.stream().mapToLong(ChapterEntity::getWordCount).sum();
        book.setChapterCount(chapters.size());
        book.setWordCount(wordCount);
        book.setLatestChapterId(latest == null ? null : latest.getId());
        book.setLatestChapterTitle(latest == null ? null : latest.getTitle());
        book.setUpdatedAt(now);
        bookMapper.updateById(book);

        return new ImportedBookResponse(Long.toUnsignedString(book.getId()), book.getTitle(), crawledBook.chapters().size());
    }

    private ContentSourceEntity findOrCreateSource(
            CrawledBook crawledBook, String sourceBaseUrl, LocalDateTime now) {
        ContentSourceEntity source = sourceMapper.findByBaseUrl(sourceBaseUrl);
        if (source != null) {
            return source;
        }
        source = new ContentSourceEntity();
        source.setSourceKey("sonovel-" + shortHash(sourceBaseUrl));
        source.setDisplayName(truncate(value(crawledBook.sourceName(), URI.create(sourceBaseUrl).getHost()), 100));
        source.setBaseUrl(sourceBaseUrl);
        source.setAdapterType("SONOVEL");
        source.setEnabled(true);
        source.setRateLimitPerMinute(20);
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
        sourceMapper.insert(source);
        return source;
    }

    private void saveChapter(long bookId, CrawledBook.CrawledChapter crawled, LocalDateTime now) {
        if (crawled.chapterIndex() < 1 || crawled.content() == null || crawled.content().isBlank()) {
            return;
        }
        String content = normalizeContent(crawled.content());
        ChapterEntity chapter = chapterMapper.selectOne(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, bookId)
                .eq(ChapterEntity::getChapterIndex, crawled.chapterIndex())
                .last("LIMIT 1"));
        if (chapter == null) {
            chapter = new ChapterEntity();
            chapter.setBookId(bookId);
            chapter.setChapterIndex(crawled.chapterIndex());
            chapter.setCreatedAt(now);
        }
        chapter.setTitle(truncate(value(crawled.title(), "第 " + crawled.chapterIndex() + " 章"), 255));
        chapter.setContent(content);
        chapter.setContentHash(hash(content));
        String chapterUrl = crawled.sourceUrl() == null ? null : normalizeUrl(crawled.sourceUrl());
        chapter.setSourceUrl(truncate(chapterUrl, 2048));
        chapter.setSourceUrlHash(chapterUrl == null ? null : hash(chapterUrl));
        String compactContent = content.replaceAll("\\s", "");
        chapter.setWordCount(compactContent.codePointCount(0, compactContent.length()));
        chapter.setUpdatedAt(now);
        if (chapter.getId() == null) {
            chapterMapper.insert(chapter);
        } else {
            chapterMapper.updateById(chapter);
        }
    }

    private void validate(CrawledBook book) {
        if (book == null || book.title() == null || book.title().isBlank()
                || book.sourceUrl() == null || book.sourceUrl().isBlank()
                || book.chapters() == null || book.chapters().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY, 50201, "INVALID_CRAWLER_RESULT", "采集结果缺少书籍或章节信息");
        }
    }

    private String normalizeContent(String content) {
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String normalizeStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.toUpperCase(Locale.ROOT);
        if (status.contains("完结") || status.contains("COMPLETED")) {
            return "COMPLETED";
        }
        if (status.contains("连载") || status.contains("ONGOING")) {
            return "ONGOING";
        }
        if (status.contains("暂停") || status.contains("PAUSED")) {
            return "PAUSED";
        }
        return "UNKNOWN";
    }

    private String normalizeBaseUrl(String rawBaseUrl, String fallbackBookUrl) {
        URI uri = URI.create(rawBaseUrl == null || rawBaseUrl.isBlank() ? fallbackBookUrl : rawBaseUrl);
        int port = uri.getPort();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (port < 0 ? "" : ":" + port) + "/";
    }

    private String normalizeUrl(String rawUrl) {
        URI uri = URI.create(rawUrl.trim()).normalize();
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 40008, "INVALID_SOURCE_URL", "来源地址不合法");
        }
        return uri.toString();
    }

    private String shortHash(String value) {
        return HexFormat.of().formatHex(hash(value)).substring(0, 16);
    }

    private byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
