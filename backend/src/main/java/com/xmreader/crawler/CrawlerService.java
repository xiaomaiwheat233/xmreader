package com.xmreader.crawler;

import com.xmreader.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CrawlerService {

    private final CrawlerGateway gateway;
    private final CrawlerCatalogWriter catalogWriter;
    private final CrawlerProperties properties;

    public CrawlerService(
            CrawlerGateway gateway,
            CrawlerCatalogWriter catalogWriter,
            CrawlerProperties properties) {
        this.gateway = gateway;
        this.catalogWriter = catalogWriter;
        this.properties = properties;
    }

    public List<OnlineBookCandidate> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, 40007, "INVALID_QUERY", "搜索关键词长度必须为 1 到 100");
        }
        return gateway.search(normalized);
    }

    public CrawledBook downloadBook(String sourceUrl) {
        return gateway.fetchBook(sourceUrl.trim());
    }

    ImportedBookResponse importBook(String sourceUrl) {
        return catalogWriter.save(gateway.fetchBook(sourceUrl.trim()));
    }
}
