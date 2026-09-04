package com.xmreader.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.shared.web.PageResponse;
import com.xmreader.reading.ReadingService;
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
    private final ReadingService readingService;

    public CatalogService(
            BookMapper bookMapper,
            ChapterMapper chapterMapper,
            ContentSourceMapper sourceMapper,
            ReadingService readingService) {
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.sourceMapper = sourceMapper;
        this.readingService = readingService;
    }

    public HomeResponse home() {
        List<BookSummaryResponse> recentlyUpdated = listBooks("UPDATED_DESC", 12);
        List<BookSummaryResponse> popular = listBooks("POPULAR_DESC", 12);
        List<BookSummaryResponse> recommended = listBooks("CREATED_DESC", 12);
        return new HomeResponse(recommended, recentlyUpdated, popular);
    }

    public PageResponse<BookSummaryResponse> list(
            Long userId,
            int page,
            int pageSize,
            String category,
            String status,
            String sort) {
        validatePage(page, pageSize, 100);
        LambdaQueryWrapper<BookEntity> query = accessibleBooks(userId);
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

    public PageResponse<BookSummaryResponse> list(
            int page, int pageSize, String category, String status, String sort) {
        return list(null, page, pageSize, category, status, sort);
    }

    public PageResponse<BookSummaryResponse> search(Long userId, String keyword, int page, int pageSize) {
        validatePage(page, pageSize, 100);
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw invalidQuery("搜索关键词长度必须为 1 到 100");
        }
        String escaped = escapeLike(normalized);
        LambdaQueryWrapper<BookEntity> query = accessibleBooks(userId)
                .and(wrapper -> wrapper
                        .apply("title LIKE CONCAT('%', {0}, '%') ESCAPE '!'", escaped)
                        .or()
                        .apply("author LIKE CONCAT('%', {0}, '%') ESCAPE '!'", escaped))
                .orderByDesc(BookEntity::getUpdatedAt)
                .orderByDesc(BookEntity::getId);
        Page<BookEntity> result = bookMapper.selectPage(Page.of(page, pageSize), query);
        return PageResponse.from(result, BookSummaryResponse::from);
    }

    public PageResponse<BookSummaryResponse> search(String keyword, int page, int pageSize) {
        return search(null, keyword, page, pageSize);
    }

    public BookDetailResponse getBook(String bookId, Long userId) {
        long parsedBookId = parseId(bookId);
        BookEntity book = requireAccessibleBook(parsedBookId, userId);
        ContentSourceEntity source = sourceMapper.selectById(book.getSourceId());
        ChapterEntity latestChapter = book.getLatestChapterId() == null
                ? null
                : chapterMapper.selectById(book.getLatestChapterId());
        boolean inBookshelf = userId != null && readingService.isInBookshelf(userId, parsedBookId);
        return BookDetailResponse.from(book, source, latestChapter, inBookshelf);
    }

    public PageResponse<ChapterSummaryResponse> listChapters(String bookId, Long userId, int page, int pageSize) {
        validatePage(page, pageSize, 200);
        long parsedBookId = parseId(bookId);
        requireAccessibleBook(parsedBookId, userId);
        LambdaQueryWrapper<ChapterEntity> query = new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getBookId, parsedBookId)
                .orderByAsc(ChapterEntity::getChapterIndex);
        Page<ChapterEntity> result = chapterMapper.selectPage(Page.of(page, pageSize), query);
        return PageResponse.from(result, ChapterSummaryResponse::from);
    }

    public PageResponse<ChapterSummaryResponse> listChapters(String bookId, int page, int pageSize) {
        return listChapters(bookId, null, page, pageSize);
    }

    public ChapterDetailResponse getChapter(String chapterId, Long userId) {
        ChapterEntity chapter = chapterMapper.selectById(parseId(chapterId));
        if (chapter == null) {
            throw notFound("章节不存在");
        }
        BookEntity book = requireAccessibleBook(chapter.getBookId(), userId);
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

    public ChapterDetailResponse getChapter(String chapterId) {
        return getChapter(chapterId, null);
    }

    private List<BookSummaryResponse> listBooks(String sort, int limit) {
        LambdaQueryWrapper<BookEntity> query = visibleBooks();
        applySort(query, sort);
        query.last("LIMIT " + limit);
        return bookMapper.selectList(query).stream().map(BookSummaryResponse::from).toList();
    }

    private LambdaQueryWrapper<BookEntity> visibleBooks() {
        return accessibleBooks(null);
    }

    private LambdaQueryWrapper<BookEntity> accessibleBooks(Long userId) {
        LambdaQueryWrapper<BookEntity> query = new LambdaQueryWrapper<BookEntity>()
                .eq(BookEntity::getVisibility, "VISIBLE")
                .isNull(BookEntity::getDeletedAt);
        if (userId == null) {
            query.eq(BookEntity::getAccessScope, "PUBLIC");
        } else {
            query.and(scope -> scope.eq(BookEntity::getAccessScope, "PUBLIC")
                    .or(owner -> owner.eq(BookEntity::getAccessScope, "PRIVATE")
                            .eq(BookEntity::getOwnerUserId, userId)));
        }
        return query;
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

    private BookEntity requireAccessibleBook(long id, Long userId) {
        BookEntity book = bookMapper.selectById(id);
        boolean privateForAnotherUser = book != null && "PRIVATE".equals(book.getAccessScope())
                && (userId == null || !userId.equals(book.getOwnerUserId()));
        if (book == null || book.getDeletedAt() != null || !"VISIBLE".equals(book.getVisibility())
                || privateForAnotherUser) {
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
