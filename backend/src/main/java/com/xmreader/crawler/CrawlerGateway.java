package com.xmreader.crawler;

import java.util.List;

public interface CrawlerGateway {

    List<OnlineBookCandidate> search(String keyword);

    CrawledBook fetchBook(String sourceUrl, int chapterLimit);
}
