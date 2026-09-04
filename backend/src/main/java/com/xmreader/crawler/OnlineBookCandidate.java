package com.xmreader.crawler;

public record OnlineBookCandidate(
        int sourceId,
        String sourceName,
        String sourceUrl,
        String title,
        String author,
        String description,
        String category,
        String latestChapterTitle,
        String updatedAtText,
        String statusText,
        String wordCountText,
        boolean importSupported) {
}
