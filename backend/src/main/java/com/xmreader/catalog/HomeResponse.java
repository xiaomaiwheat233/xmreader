package com.xmreader.catalog;

import java.util.List;

public record HomeResponse(
        List<BookSummaryResponse> recommended,
        List<BookSummaryResponse> recentlyUpdated,
        List<BookSummaryResponse> popular) {
}
