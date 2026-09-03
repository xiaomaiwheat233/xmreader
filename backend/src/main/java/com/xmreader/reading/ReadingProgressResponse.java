package com.xmreader.reading;

import com.xmreader.catalog.ChapterEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record ReadingProgressResponse(
        String bookId,
        String chapterId,
        int chapterIndex,
        String chapterTitle,
        int position,
        BigDecimal progressPercent,
        Instant updatedAt) {

    public static ReadingProgressResponse from(ReadingProgressEntity progress, ChapterEntity chapter) {
        return new ReadingProgressResponse(
                Long.toUnsignedString(progress.getBookId()),
                Long.toUnsignedString(progress.getChapterId()),
                chapter.getChapterIndex(),
                chapter.getTitle(),
                progress.getPosition(),
                progress.getProgressPercent(),
                progress.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
