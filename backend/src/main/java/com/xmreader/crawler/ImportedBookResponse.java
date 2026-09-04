package com.xmreader.crawler;

public record ImportedBookResponse(String bookId, String title, int importedChapterCount) {
}
