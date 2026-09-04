package com.xmreader.crawler;

public record ImportTaskResponse(
        String taskId,
        String status,
        String bookId,
        String title,
        Integer importedChapterCount,
        String errorMessage) {
}
