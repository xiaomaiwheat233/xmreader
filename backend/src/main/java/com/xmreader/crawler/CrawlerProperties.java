package com.xmreader.crawler;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.crawler")
public record CrawlerProperties(
        boolean enabled,
        URI baseUrl,
        boolean lightNovelEnabled,
        int searchLimit,
        int connectTimeoutSeconds,
        int readTimeoutSeconds) {
}
