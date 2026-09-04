package com.xmreader.crawler;

import com.xmreader.shared.exception.BusinessException;
import java.time.Duration;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SoNovelCrawlerGateway implements CrawlerGateway {

    private final CrawlerProperties properties;
    private final RestClient restClient;

    public SoNovelCrawlerGateway(CrawlerProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<OnlineBookCandidate> search(String keyword) {
        requireEnabled();
        try {
            RemoteEnvelope<List<RemoteSearchResult>> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/aggregated")
                            .queryParam("kw", keyword)
                            .queryParam("searchLimit", properties.searchLimit())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.code() != 200 || response.data() == null) {
                throw unavailable(response == null ? "Adapter 返回空响应" : response.message());
            }
            return response.data().stream()
                    .filter(item -> item.url() != null && !item.url().isBlank())
                    .map(item -> new OnlineBookCandidate(
                            item.sourceId() == null ? 0 : item.sourceId(),
                            value(item.sourceName(), "未知来源"),
                            item.url(),
                            value(item.bookName(), "未命名小说"),
                            value(item.author(), "未知作者"),
                            item.intro(),
                            item.category(),
                            item.latestChapter(),
                            item.lastUpdateTime(),
                            item.status(),
                            item.wordCount()))
                    .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable("联网搜索服务暂不可用");
        }
    }

    @Override
    public CrawledBook fetchBook(String sourceUrl, int chapterLimit) {
        requireEnabled();
        try {
            RemoteEnvelope<CrawledBook> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/xmreader/book")
                            .queryParam("url", sourceUrl)
                            .queryParam("limit", chapterLimit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.code() != 200 || response.data() == null) {
                throw unavailable(response == null ? "Adapter 返回空响应" : response.message());
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable("小说抓取失败，请稍后重试或更换来源");
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw unavailable("联网采集功能未启用");
        }
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, 50301, "CRAWLER_UNAVAILABLE", message);
    }

    private record RemoteEnvelope<T>(int code, String message, T data) {
    }

    private record RemoteSearchResult(
            Integer sourceId,
            String sourceName,
            String url,
            String bookName,
            String author,
            String intro,
            String category,
            String latestChapter,
            String lastUpdateTime,
            String status,
            String wordCount) {
    }
}
