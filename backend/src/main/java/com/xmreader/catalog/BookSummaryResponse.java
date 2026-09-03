package com.xmreader.catalog;

import java.time.Instant;
import java.time.ZoneOffset;

public record BookSummaryResponse(
        String id,
        String title,
        String author,
        String coverUrl,
        String description,
        String category,
        String status,
        long wordCount,
        int chapterCount,
        String latestChapterId,
        String latestChapterTitle,
        Instant updatedAt) {

    public static BookSummaryResponse from(BookEntity book) {
        return new BookSummaryResponse(
                Long.toUnsignedString(book.getId()),
                book.getTitle(),
                book.getAuthor(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getCategory(),
                book.getStatus(),
                book.getWordCount(),
                book.getChapterCount(),
                book.getLatestChapterId() == null ? null : Long.toUnsignedString(book.getLatestChapterId()),
                book.getLatestChapterTitle(),
                book.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
