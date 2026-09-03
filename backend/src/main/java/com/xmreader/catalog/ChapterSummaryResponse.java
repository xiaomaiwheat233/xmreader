package com.xmreader.catalog;

import java.time.Instant;
import java.time.ZoneOffset;

public record ChapterSummaryResponse(
        String id,
        int chapterIndex,
        String title,
        int wordCount,
        Instant publishedAt) {

    public static ChapterSummaryResponse from(ChapterEntity chapter) {
        return new ChapterSummaryResponse(
                Long.toUnsignedString(chapter.getId()),
                chapter.getChapterIndex(),
                chapter.getTitle(),
                chapter.getWordCount(),
                chapter.getPublishedAt() == null ? null : chapter.getPublishedAt().toInstant(ZoneOffset.UTC));
    }
}
