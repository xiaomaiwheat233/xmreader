package com.xmreader.catalog;

import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.PageResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/home")
    public ApiResponse<HomeResponse> home(HttpServletRequest request) {
        return ApiResponse.success(catalogService.home(), requestId(request));
    }

    @GetMapping("/books")
    public ApiResponse<PageResponse<BookSummaryResponse>> books(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "UPDATED_DESC") String sort,
            HttpServletRequest request) {
        return ApiResponse.success(
                catalogService.list(page, pageSize, category, status, sort), requestId(request));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<BookSummaryResponse>> search(
            @RequestParam(name = "q") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.search(keyword, page, pageSize), requestId(request));
    }

    @GetMapping("/books/{bookId}")
    public ApiResponse<BookDetailResponse> book(
            @PathVariable String bookId,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.getBook(bookId), requestId(request));
    }

    @GetMapping("/books/{bookId}/chapters")
    public ApiResponse<PageResponse<ChapterSummaryResponse>> chapters(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.listChapters(bookId, page, pageSize), requestId(request));
    }

    @GetMapping("/chapters/{chapterId}")
    public ApiResponse<ChapterDetailResponse> chapter(
            @PathVariable String chapterId,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.getChapter(chapterId), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
