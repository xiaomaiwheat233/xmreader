package com.xmreader.reading;

import com.xmreader.catalog.BookEntity;
import com.xmreader.catalog.BookSummaryResponse;
import com.xmreader.catalog.ChapterEntity;
import java.time.Instant;
import java.time.ZoneOffset;

public record BookshelfItemResponse(
        BookSummaryResponse book,
        ReadingProgressResponse progress,
        Instant addedAt) {

    public static BookshelfItemResponse from(
            BookshelfEntity shelf,
            BookEntity book,
            ReadingProgressEntity progress,
            ChapterEntity chapter) {
        return new BookshelfItemResponse(
                BookSummaryResponse.from(book),
                progress == null ? null : ReadingProgressResponse.from(progress, chapter),
                shelf.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}
