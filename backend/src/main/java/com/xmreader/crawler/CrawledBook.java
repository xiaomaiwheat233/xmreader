package com.xmreader.crawler;

import java.util.List;

public record CrawledBook(
        int sourceId,
        String sourceName,
        String sourceBaseUrl,
        String sourceUrl,
        String title,
        String author,
        String description,
        String coverUrl,
        String category,
        String status,
        String latestChapterTitle,
        List<CrawledChapter> chapters) {

    public record CrawledChapter(int chapterIndex, String title, String sourceUrl, String content) {
    }
}
