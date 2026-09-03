package com.xmreader.reading;

import com.xmreader.catalog.BookEntity;
import com.xmreader.catalog.BookSummaryResponse;
import com.xmreader.catalog.ChapterEntity;
import java.time.Instant;
import java.time.ZoneOffset;

public record ReadingHistoryItemResponse(
        BookSummaryResponse book,
        ChapterReference chapter,
        Instant visitedAt) {

    public static ReadingHistoryItemResponse from(
            ReadingHistoryEntity history,
            BookEntity book,
            ChapterEntity chapter) {
        return new ReadingHistoryItemResponse(
                BookSummaryResponse.from(book),
                new ChapterReference(
                        Long.toUnsignedString(chapter.getId()),
                        chapter.getChapterIndex(),
                        chapter.getTitle()),
                history.getVisitedAt().toInstant(ZoneOffset.UTC));
    }

    public record ChapterReference(String id, int chapterIndex, String title) {
    }
}
