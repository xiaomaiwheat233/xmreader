package com.xmreader.reading;

import java.time.Instant;
import java.time.ZoneOffset;

public record RemoteBookshelfItemResponse(
        String id,
        int sourceId,
        String sourceName,
        String sourceUrl,
        String title,
        String author,
        String description,
        String coverUrl,
        String category,
        String latestChapterTitle,
        String statusText,
        boolean importSupported,
        String importedBookId,
        Instant addedAt) {
    public static RemoteBookshelfItemResponse from(RemoteBookshelfEntity item) {
        return new RemoteBookshelfItemResponse(
                Long.toUnsignedString(item.getId()), item.getSourceId(), item.getSourceName(), item.getSourceUrl(),
                item.getTitle(), item.getAuthor(), item.getDescription(), item.getCoverUrl(), item.getCategory(),
                item.getLatestChapterTitle(), item.getStatusText(), Boolean.TRUE.equals(item.getImportSupported()),
                item.getImportedBookId() == null ? null : Long.toUnsignedString(item.getImportedBookId()),
                item.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}
