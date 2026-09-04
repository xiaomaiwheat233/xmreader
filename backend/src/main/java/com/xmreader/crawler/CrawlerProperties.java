package com.xmreader.crawler;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.crawler")
public record CrawlerProperties(
        boolean enabled,
        URI baseUrl,
        int searchLimit,
        int importChapterLimit,
        int connectTimeoutSeconds,
        int readTimeoutSeconds) {
}
