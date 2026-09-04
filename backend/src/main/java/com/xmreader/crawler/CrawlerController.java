package com.xmreader.crawler;

import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/search")
    public ApiResponse<List<OnlineBookCandidate>> search(
            @RequestParam(name = "q") String keyword,
            HttpServletRequest request) {
        return ApiResponse.success(crawlerService.search(keyword), requestId(request));
    }

    @PostMapping("/imports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportedBookResponse> importBook(
            @Valid @RequestBody ImportBookRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(crawlerService.importBook(body.sourceUrl()), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
