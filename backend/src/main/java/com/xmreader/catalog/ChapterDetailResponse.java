package com.xmreader.catalog;

import java.time.Instant;
import java.time.ZoneOffset;

public record ChapterDetailResponse(
        String id,
        BookReference book,
        int chapterIndex,
        String title,
        String content,
        int wordCount,
        String previousChapterId,
        String nextChapterId,
        Instant updatedAt) {

    public static ChapterDetailResponse from(
            ChapterEntity chapter,
            BookEntity book,
            ChapterEntity previous,
            ChapterEntity next) {
        return new ChapterDetailResponse(
                Long.toUnsignedString(chapter.getId()),
                new BookReference(Long.toUnsignedString(book.getId()), book.getTitle()),
                chapter.getChapterIndex(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getWordCount(),
                previous == null ? null : Long.toUnsignedString(previous.getId()),
                next == null ? null : Long.toUnsignedString(next.getId()),
                chapter.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    public record BookReference(String id, String title) {
    }
}
