package com.xmreader.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.shared.web.PageResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private static final Set<String> BOOK_STATUSES = Set.of("ONGOING", "COMPLETED", "PAUSED", "UNKNOWN");

    private final BookMapper bookMapper;
    private final ChapterMapper chapterMapper;
    private final ContentSourceMapper sourceMapper;

    public CatalogService(BookMapper bookMapper, ChapterMapper chapterMapper, ContentSourceMapper sourceMapper) {
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.sourceMapper = sourceMapper;
    }

    public HomeResponse home() {
        List<BookSummaryResponse> recentlyUpdated = listBooks("UPDATED_DESC", 12);
        List<BookSummaryResponse> popular = listBooks("POPULAR_DESC", 12);
        List<BookSummaryResponse> recommended = listBooks("CREATED_DESC", 12);
        return new HomeResponse(recommended, recentlyUpdated, popular);
    }

    public PageResponse<BookSummaryResponse> list(
            int page,
            int pageSize,
            String category,
            String status,
            String sort) {
        validatePage(page, pageSize, 100);
        LambdaQueryWrapper<BookEntity> query = visibleBooks();
        if (category != null && !category.isBlank()) {
            query.eq(BookEntity::getCategory, category.trim());
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (!BOOK_STATUSES.contains(normalizedStatus)) {
                throw invalidQuery("status 参数不正确");
            }
            query.eq(BookEntity::getStatus, normalizedStatus);
        }
        applySort(query, sort);
        Page<BookEntity> result = bookMapper.selectPage(Page.of(page, pageSize), query);
        return PageResponse.from(result, BookSummaryResponse::from);
    }

    public PageResponse<BookSummaryResponse> search(String keyword, int page, int pageSize) {
        validatePage(page, pageSize, 100);
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw invalidQuery("搜索关键词长度必须为 1 到 100");
        }
        String escaped = escapeLike(normalized);
        LambdaQueryWrapper<BookEntity> query = visibleBooks()
                .and(wrapper -> wrapper
                        .apply("title LIKE CONCAT('%', {0}, '%') ESCAPE '!'", escaped)
                        .or()
                        .apply("author LIKE CONCAT('%', {0}, '%') ESCAPE '!'", escaped))
                .orderByDesc(BookEntity::getUpdatedAt)
                .orderByDesc(BookEntity::getId);
        Page<BookEntity> result = bookMapper.selectPage(Page.of(page, pageSize), query);
        return PageResponse.from(result, BookSummaryResponse::from);
    }

    public BookDetailResponse getBook(String bookId) {
        BookEntity book = requireVisibleBook(parseId(bookId));
        ContentSourceEntity source = sourceMapper.selectById(book.getSourceId());
        ChapterEntity latestChapter = book.getLatestChapterId() == null
                ? null
                : chapterMapper.selectById(book.getLatestChapterId());
        return BookDetailResponse.from(book, source, latestChapter);
    }

    public PageResponse<ChapterSummaryResponse> listChapters(String bookId, int page, int pageSize) {
        validatePage(page, pageSize, 200);
        long parsedBookId = parseId(bookId);
        requireVisibleBook(parsedBookId);
        LambdaQueryWrapper<ChapterEntity> query = new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, parsedBookId)
                .orderByAsc(ChapterEntity::getChapterIndex);
        Page<ChapterEntity> result = chapterMapper.selectPage(Page.of(page, pageSize), query);
        return PageResponse.from(result, ChapterSummaryResponse::from);
    }

    public ChapterDetailResponse getChapter(String chapterId) {
        ChapterEntity chapter = chapterMapper.selectById(parseId(chapterId));
        if (chapter == null) {
            throw notFound("章节不存在");
        }
        BookEntity book = requireVisibleBook(chapter.getBookId());
        ChapterEntity previous = chapterMapper.selectOne(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, book.getId())
                .lt(ChapterEntity::getChapterIndex, chapter.getChapterIndex())
                .orderByDesc(ChapterEntity::getChapterIndex)
                .last("LIMIT 1"));
        ChapterEntity next = chapterMapper.selectOne(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, book.getId())
                .gt(ChapterEntity::getChapterIndex, chapter.getChapterIndex())
                .orderByAsc(ChapterEntity::getChapterIndex)
                .last("LIMIT 1"));
        return ChapterDetailResponse.from(chapter, book, previous, next);
    }

    private List<BookSummaryResponse> listBooks(String sort, int limit) {
        LambdaQueryWrapper<BookEntity> query = visibleBooks();
        applySort(query, sort);
        query.last("LIMIT " + limit);
        return bookMapper.selectList(query).stream().map(BookSummaryResponse::from).toList();
    }

    private LambdaQueryWrapper<BookEntity> visibleBooks() {
        return new LambdaQueryWrapper<BookEntity>()
                .eq(BookEntity::getVisibility, "VISIBLE")
                .isNull(BookEntity::getDeletedAt);
    }

    private void applySort(LambdaQueryWrapper<BookEntity> query, String rawSort) {
        String sort = rawSort == null ? "UPDATED_DESC" : rawSort.toUpperCase(Locale.ROOT);
        switch (sort) {
            case "UPDATED_DESC" -> query.orderByDesc(BookEntity::getUpdatedAt);
            case "CREATED_DESC" -> query.orderByDesc(BookEntity::getCreatedAt);
            case "POPULAR_DESC" -> query.orderByDesc(BookEntity::getChapterCount)
                    .orderByDesc(BookEntity::getUpdatedAt);
            default -> throw invalidQuery("sort 参数不正确");
        }
        query.orderByDesc(BookEntity::getId);
    }

    private BookEntity requireVisibleBook(long id) {
        BookEntity book = bookMapper.selectById(id);
        if (book == null || book.getDeletedAt() != null || !"VISIBLE".equals(book.getVisibility())) {
            throw notFound("小说不存在");
        }
        return book;
    }

    private long parseId(String rawId) {
        try {
            return Long.parseUnsignedLong(rawId);
        } catch (NumberFormatException exception) {
            throw invalidQuery("资源 ID 格式不正确");
        }
    }

    private void validatePage(int page, int pageSize, int maxPageSize) {
        if (page < 1 || pageSize < 1 || pageSize > maxPageSize) {
            throw invalidQuery("分页参数超出允许范围");
        }
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private BusinessException invalidQuery(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, 40007, "INVALID_QUERY", message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, 40402, "CONTENT_NOT_FOUND", message);
    }
}
