package com.xmreader.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xmreader.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final RestClient lightNovelClient;
    private volatile List<LightNovelBook> lightNovelCache = List.of();
    private volatile Instant lightNovelCacheExpiresAt = Instant.EPOCH;

    public SoNovelCrawlerGateway(CrawlerProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        SimpleClientHttpRequestFactory lightNovelRequestFactory = new SimpleClientHttpRequestFactory();
        lightNovelRequestFactory.setConnectTimeout(Duration.ofSeconds(5));
        lightNovelRequestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.lightNovelClient = RestClient.builder()
                .baseUrl("https://lnovel.animes.garden")
                .requestFactory(lightNovelRequestFactory)
                .build();
    }

    @Override
    public List<OnlineBookCandidate> search(String keyword) {
        requireEnabled();
        List<OnlineBookCandidate> combined = new ArrayList<>();
        RuntimeException adapterFailure = null;
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
            combined.addAll(response.data().stream()
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
                            item.wordCount(),
                            true))
                    .toList());
        } catch (BusinessException exception) {
            adapterFailure = exception;
        } catch (RestClientException exception) {
            adapterFailure = exception;
        }
        try {
            if (properties.lightNovelEnabled()) combined.addAll(searchLightNovels(keyword));
        } catch (RuntimeException exception) {
            if (combined.isEmpty() && adapterFailure != null) throw unavailable("联网搜索服务暂不可用");
        }
        return combined.stream().limit(properties.searchLimit() + 10L).toList();
    }

    private List<OnlineBookCandidate> searchLightNovels(String keyword) {
        List<LightNovelBook> catalog = lightNovelCatalog();
        String needle = keyword.toLowerCase(Locale.ROOT);
        return catalog.stream()
                .filter(book -> !book.isDeleted())
                .filter(book -> book.name().toLowerCase(Locale.ROOT).contains(needle)
                        || book.authors().stream().anyMatch(author -> author.name().toLowerCase(Locale.ROOT).contains(needle)))
                .limit(10)
                .map(book -> new OnlineBookCandidate(
                        9001, "哔哩轻小说", "https://www.linovelib.com/novel/" + book.nid() + ".html",
                        book.name(), book.authors().stream().filter(a -> "author".equals(a.position()))
                                .map(LightNovelAuthor::name).findFirst().orElse("未知作者"),
                        stripHtml(book.description()),
                        book.labels().stream().skip(1).findFirst().orElse("轻小说"), null,
                        book.updatedAt(), book.labels().stream().findFirst().orElse(null), null,
                        false))
                .toList();
    }

    private synchronized List<LightNovelBook> lightNovelCatalog() {
        if (Instant.now().isBefore(lightNovelCacheExpiresAt) && !lightNovelCache.isEmpty()) return lightNovelCache;
        LightNovelEnvelope<List<LightNovelBook>> response = lightNovelClient.get().uri("/bili/novels")
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || !response.ok() || response.data() == null) throw unavailable("轻小说索引暂不可用");
        lightNovelCache = response.data();
        lightNovelCacheExpiresAt = Instant.now().plus(Duration.ofMinutes(30));
        return lightNovelCache;
    }

    private String stripHtml(String value) {
        return value == null ? null : value.replaceAll("<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").trim();
    }

    @Override
    public CrawledBook fetchBook(String sourceUrl) {
        requireEnabled();
        try {
            RemoteEnvelope<CrawledBook> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/xmreader/book")
                            .queryParam("url", sourceUrl)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LightNovelEnvelope<T>(boolean ok, T data) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LightNovelBook(
            int nid, String name, List<LightNovelAuthor> authors, String description, String cover,
            List<String> labels, String updatedAt, boolean isDeleted) {
        private LightNovelBook {
            authors = authors == null ? List.of() : authors;
            labels = labels == null ? List.of() : labels;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LightNovelAuthor(String name, String position) { }
}
