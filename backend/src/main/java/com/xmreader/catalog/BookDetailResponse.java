package com.xmreader.catalog;

import java.time.Instant;
import java.time.ZoneOffset;

public record BookDetailResponse(
        String id,
        String title,
        String author,
        String coverUrl,
        String description,
        String category,
        String status,
        long wordCount,
        int chapterCount,
        LatestChapter latestChapter,
        Source source,
        boolean inBookshelf,
        Instant lastCrawledAt,
        Instant updatedAt) {

    public static BookDetailResponse from(
            BookEntity book,
            ContentSourceEntity source,
            ChapterEntity latestChapter,
            boolean inBookshelf) {
        LatestChapter latest = latestChapter == null ? null : new LatestChapter(
                Long.toUnsignedString(latestChapter.getId()),
                latestChapter.getTitle(),
                latestChapter.getChapterIndex());
        return new BookDetailResponse(
                Long.toUnsignedString(book.getId()),
                book.getTitle(),
                book.getAuthor(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getCategory(),
                book.getStatus(),
                book.getWordCount(),
                book.getChapterCount(),
                latest,
                new Source(source.getSourceKey(), source.getDisplayName()),
                inBookshelf,
                book.getLastCrawledAt() == null ? null : book.getLastCrawledAt().toInstant(ZoneOffset.UTC),
                book.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    public record LatestChapter(String id, String title, int chapterIndex) {
    }

    public record Source(String key, String name) {
    }
}
