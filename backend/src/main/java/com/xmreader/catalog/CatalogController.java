package com.xmreader.catalog;

import com.xmreader.shared.web.ApiResponse;
import com.xmreader.shared.web.PageResponse;
import com.xmreader.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import com.xmreader.crawler.ImportedBookResponse;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;
    private final BookUploadService bookUploadService;

    public CatalogController(CatalogService catalogService, BookUploadService bookUploadService) {
        this.catalogService = catalogService;
        this.bookUploadService = bookUploadService;
    }

    @PostMapping(value = "/books/uploads", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportedBookResponse> upload(
            @RequestPart(required = false) String title,
            @RequestPart(required = false) String author,
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(bookUploadService.upload(userId(jwt), title, author, file), requestId(request));
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
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(
                catalogService.list(userId(jwt), page, pageSize, category, status, sort), requestId(request));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<BookSummaryResponse>> search(
            @RequestParam(name = "q") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.search(userId(jwt), keyword, page, pageSize), requestId(request));
    }

    @GetMapping("/books/{bookId}")
    public ApiResponse<BookDetailResponse> book(
            @PathVariable String bookId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.getBook(bookId, userId(jwt)), requestId(request));
    }

    @GetMapping("/books/{bookId}/chapters")
    public ApiResponse<PageResponse<ChapterSummaryResponse>> chapters(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.listChapters(bookId, userId(jwt), page, pageSize), requestId(request));
    }

    @GetMapping("/chapters/{chapterId}")
    public ApiResponse<ChapterDetailResponse> chapter(
            @PathVariable String chapterId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiResponse.success(catalogService.getChapter(chapterId, userId(jwt)), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }

    private Long userId(Jwt jwt) {
        return jwt == null ? null : Long.parseUnsignedLong(jwt.getSubject());
    }
}
