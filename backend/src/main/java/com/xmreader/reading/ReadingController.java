package com.xmreader.reading;

import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.PageResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReadingController {

    private final ReadingService readingService;
    private final RemoteBookshelfService remoteBookshelfService;

    public ReadingController(ReadingService readingService, RemoteBookshelfService remoteBookshelfService) {
        this.readingService = readingService;
        this.remoteBookshelfService = remoteBookshelfService;
    }

    @GetMapping("/remote-bookshelf")
    public ApiResponse<PageResponse<RemoteBookshelfItemResponse>> remoteBookshelf(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(remoteBookshelfService.list(userId(jwt), page, pageSize), requestId(request));
    }

    @PutMapping("/remote-bookshelf")
    public ApiResponse<RemoteBookshelfItemResponse> saveRemoteBook(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveRemoteBookRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(remoteBookshelfService.save(userId(jwt), body), requestId(request));
    }

    @DeleteMapping("/remote-bookshelf/{itemId}")
    public ApiResponse<Void> removeRemoteBook(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId,
            HttpServletRequest request) {
        remoteBookshelfService.remove(userId(jwt), itemId);
        return ApiResponse.success(null, requestId(request));
    }

    @GetMapping("/bookshelf")
    public ApiResponse<PageResponse<BookshelfItemResponse>> bookshelf(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(readingService.bookshelf(userId(jwt), page, pageSize), requestId(request));
    }

    @PutMapping("/bookshelf/{bookId}")
    public ApiResponse<Void> addToBookshelf(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookId,
            HttpServletRequest request) {
        readingService.addToBookshelf(userId(jwt), bookId);
        return ApiResponse.success(null, requestId(request));
    }

    @DeleteMapping("/bookshelf/{bookId}")
    public ApiResponse<Void> removeFromBookshelf(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookId,
            HttpServletRequest request) {
        readingService.removeFromBookshelf(userId(jwt), bookId);
        return ApiResponse.success(null, requestId(request));
    }

    @GetMapping("/reading-progress/{bookId}")
    public ApiResponse<ReadingProgressResponse> progress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookId,
            HttpServletRequest request) {
        return ApiResponse.success(readingService.progress(userId(jwt), bookId), requestId(request));
    }

    @PutMapping("/reading-progress")
    public ApiResponse<ReadingProgressResponse> saveProgress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateReadingProgressRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(readingService.saveProgress(userId(jwt), body), requestId(request));
    }

    @GetMapping("/reading-history")
    public ApiResponse<PageResponse<ReadingHistoryItemResponse>> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(readingService.history(userId(jwt), page, pageSize), requestId(request));
    }

    @DeleteMapping("/reading-history/{bookId}")
    public ApiResponse<Void> removeHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookId,
            HttpServletRequest request) {
        readingService.removeHistory(userId(jwt), bookId);
        return ApiResponse.success(null, requestId(request));
    }

    private long userId(Jwt jwt) {
        return Long.parseUnsignedLong(jwt.getSubject());
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
