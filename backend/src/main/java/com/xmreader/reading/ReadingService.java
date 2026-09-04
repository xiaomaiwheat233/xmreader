package com.xmreader.reading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmreader.catalog.BookEntity;
import com.xmreader.catalog.BookMapper;
import com.xmreader.catalog.ChapterEntity;
import com.xmreader.catalog.ChapterMapper;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.shared.web.PageResponse;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingService {

    private final BookshelfMapper bookshelfMapper;
    private final ReadingProgressMapper progressMapper;
    private final ReadingHistoryMapper historyMapper;
    private final BookMapper bookMapper;
    private final ChapterMapper chapterMapper;
    private final Clock clock;

    public ReadingService(
            BookshelfMapper bookshelfMapper,
            ReadingProgressMapper progressMapper,
            ReadingHistoryMapper historyMapper,
            BookMapper bookMapper,
            ChapterMapper chapterMapper,
            Clock clock) {
        this.bookshelfMapper = bookshelfMapper;
        this.progressMapper = progressMapper;
        this.historyMapper = historyMapper;
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.clock = clock;
    }

    public PageResponse<BookshelfItemResponse> bookshelf(long userId, int page, int pageSize) {
        validatePage(page, pageSize);
        Page<BookshelfEntity> result = bookshelfMapper.selectPage(
                Page.of(page, pageSize),
                new LambdaQueryWrapper<BookshelfEntity>()
                        .eq(BookshelfEntity::getUserId, userId)
                        .orderByDesc(BookshelfEntity::getCreatedAt)
                        .orderByDesc(BookshelfEntity::getId));
        List<BookshelfItemResponse> items = result.getRecords().stream()
                .map(shelf -> bookshelfItem(userId, shelf))
                .toList();
        return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional
    public void addToBookshelf(long userId, String rawBookId) {
        long bookId = parseId(rawBookId);
        requireVisibleBook(bookId, userId);
        if (findShelf(userId, bookId) != null) {
            return;
        }
        BookshelfEntity shelf = new BookshelfEntity();
        shelf.setUserId(userId);
        shelf.setBookId(bookId);
        shelf.setCreatedAt(now());
        bookshelfMapper.insert(shelf);
    }

    @Transactional
    public void removeFromBookshelf(long userId, String rawBookId) {
        bookshelfMapper.delete(new LambdaQueryWrapper<BookshelfEntity>()
                .eq(BookshelfEntity::getUserId, userId)
                .eq(BookshelfEntity::getBookId, parseId(rawBookId)));
    }

    public ReadingProgressResponse progress(long userId, String rawBookId) {
        long bookId = parseId(rawBookId);
        requireVisibleBook(bookId, userId);
        ReadingProgressEntity progress = findProgress(userId, bookId);
        if (progress == null) {
            return null;
        }
        ChapterEntity chapter = requireChapter(progress.getChapterId());
        return ReadingProgressResponse.from(progress, chapter);
    }

    @Transactional
    public ReadingProgressResponse saveProgress(long userId, UpdateReadingProgressRequest request) {
        long bookId = parseId(request.bookId());
        long chapterId = parseId(request.chapterId());
        requireVisibleBook(bookId, userId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getBookId().equals(bookId)) {
            throw invalidQuery("章节不属于指定小说");
        }
        if (request.position() > chapter.getContent().length()) {
            throw invalidQuery("章节阅读位置超出正文范围");
        }

        LocalDateTime now = now();
        ReadingProgressEntity progress = findProgress(userId, bookId);
        if (progress == null) {
            progress = new ReadingProgressEntity();
            progress.setUserId(userId);
            progress.setBookId(bookId);
            progress.setChapterId(chapterId);
            progress.setPosition(request.position());
            progress.setProgressPercent(request.progressPercent().setScale(2, RoundingMode.HALF_UP));
            progress.setUpdatedAt(now);
            progressMapper.insert(progress);
        } else {
            progress.setChapterId(chapterId);
            progress.setPosition(request.position());
            progress.setProgressPercent(request.progressPercent().setScale(2, RoundingMode.HALF_UP));
            progress.setUpdatedAt(now);
            progressMapper.updateById(progress);
        }
        saveHistory(userId, bookId, chapterId, now);
        return ReadingProgressResponse.from(progress, chapter);
    }

    public PageResponse<ReadingHistoryItemResponse> history(long userId, int page, int pageSize) {
        validatePage(page, pageSize);
        Page<ReadingHistoryEntity> result = historyMapper.selectPage(
                Page.of(page, pageSize),
                new LambdaQueryWrapper<ReadingHistoryEntity>()
                        .eq(ReadingHistoryEntity::getUserId, userId)
                        .orderByDesc(ReadingHistoryEntity::getVisitedAt)
                        .orderByDesc(ReadingHistoryEntity::getId));
        List<ReadingHistoryItemResponse> items = result.getRecords().stream()
                .map(item -> ReadingHistoryItemResponse.from(
                        item,
                        requireVisibleBook(item.getBookId(), userId),
                        requireChapter(item.getChapterId())))
                .toList();
        return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional
    public void removeHistory(long userId, String rawBookId) {
        historyMapper.delete(new LambdaQueryWrapper<ReadingHistoryEntity>()
                .eq(ReadingHistoryEntity::getUserId, userId)
                .eq(ReadingHistoryEntity::getBookId, parseId(rawBookId)));
    }

    public boolean isInBookshelf(long userId, long bookId) {
        return findShelf(userId, bookId) != null;
    }

    private BookshelfItemResponse bookshelfItem(long userId, BookshelfEntity shelf) {
        BookEntity book = requireVisibleBook(shelf.getBookId(), userId);
        ReadingProgressEntity progress = findProgress(userId, shelf.getBookId());
        ChapterEntity chapter = progress == null ? null : requireChapter(progress.getChapterId());
        return BookshelfItemResponse.from(shelf, book, progress, chapter);
    }

    private void saveHistory(long userId, long bookId, long chapterId, LocalDateTime visitedAt) {
        ReadingHistoryEntity history = historyMapper.selectOne(new LambdaQueryWrapper<ReadingHistoryEntity>()
                .eq(ReadingHistoryEntity::getUserId, userId)
                .eq(ReadingHistoryEntity::getBookId, bookId));
        if (history == null) {
            history = new ReadingHistoryEntity();
            history.setUserId(userId);
            history.setBookId(bookId);
            history.setChapterId(chapterId);
            history.setVisitedAt(visitedAt);
            historyMapper.insert(history);
        } else {
            history.setChapterId(chapterId);
            history.setVisitedAt(visitedAt);
            historyMapper.updateById(history);
        }
    }

    private BookshelfEntity findShelf(long userId, long bookId) {
        return bookshelfMapper.selectOne(new LambdaQueryWrapper<BookshelfEntity>()
                .eq(BookshelfEntity::getUserId, userId)
                .eq(BookshelfEntity::getBookId, bookId));
    }

    private ReadingProgressEntity findProgress(long userId, long bookId) {
        return progressMapper.selectOne(new LambdaQueryWrapper<ReadingProgressEntity>()
                .eq(ReadingProgressEntity::getUserId, userId)
                .eq(ReadingProgressEntity::getBookId, bookId));
    }

    private BookEntity requireVisibleBook(long bookId, long userId) {
        BookEntity book = bookMapper.selectById(bookId);
        boolean privateForAnotherUser = book != null && "PRIVATE".equals(book.getAccessScope())
                && !Long.valueOf(userId).equals(book.getOwnerUserId());
        if (book == null || book.getDeletedAt() != null || !"VISIBLE".equals(book.getVisibility())
                || privateForAnotherUser) {
            throw notFound("小说不存在");
        }
        return book;
    }

    private ChapterEntity requireChapter(long chapterId) {
        ChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw notFound("章节不存在");
        }
        return chapter;
    }

    private long parseId(String rawId) {
        try {
            return Long.parseUnsignedLong(rawId);
        } catch (NumberFormatException exception) {
            throw invalidQuery("资源 ID 格式不正确");
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw invalidQuery("分页参数超出允许范围");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private BusinessException invalidQuery(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, 40007, "INVALID_QUERY", message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, 40402, "CONTENT_NOT_FOUND", message);
    }
}
