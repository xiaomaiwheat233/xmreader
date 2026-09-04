package com.xmreader.crawler;

import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ImportTaskManager importTaskManager;

    public CrawlerController(CrawlerService crawlerService, ImportTaskManager importTaskManager) {
        this.crawlerService = crawlerService;
        this.importTaskManager = importTaskManager;
    }

    @GetMapping("/search")
    public ApiResponse<List<OnlineBookCandidate>> search(
            @RequestParam(name = "q") String keyword,
            HttpServletRequest request) {
        return ApiResponse.success(crawlerService.search(keyword), requestId(request));
    }

    @PostMapping("/imports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportTaskResponse> importBook(
            @Valid @RequestBody ImportBookRequest body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(importTaskManager.start(userId(jwt), body.sourceUrl()), requestId(request));
    }

    @GetMapping("/imports/{taskId}")
    public ApiResponse<ImportTaskResponse> importStatus(
            @PathVariable String taskId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(importTaskManager.get(userId(jwt), taskId), requestId(request));
    }

    @PostMapping("/downloads")
    public ResponseEntity<byte[]> download(@Valid @RequestBody ImportBookRequest body) {
        CrawledBook book = crawlerService.downloadBook(body.sourceUrl());
        StringBuilder text = new StringBuilder();
        text.append(book.title()).append("\n作者：").append(book.author()).append("\n来源：")
                .append(book.sourceUrl()).append("\n\n");
        for (CrawledBook.CrawledChapter chapter : book.chapters()) {
            text.append(chapter.title()).append("\n\n").append(chapter.content()).append("\n\n");
        }
        String filename = book.title().replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(text.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }

    private long userId(Jwt jwt) {
        return Long.parseUnsignedLong(jwt.getSubject());
    }
}
