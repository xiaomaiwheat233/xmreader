package com.xmreader.reading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmreader.shared.exception.BusinessException;
import com.xmreader.shared.web.PageResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoteBookshelfService {
    private final RemoteBookshelfMapper mapper;
    private final Clock clock;

    public RemoteBookshelfService(RemoteBookshelfMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public PageResponse<RemoteBookshelfItemResponse> list(long userId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 40007, "INVALID_QUERY", "分页参数不正确");
        }
        Page<RemoteBookshelfEntity> result = mapper.selectPage(Page.of(page, pageSize),
                new LambdaQueryWrapper<RemoteBookshelfEntity>().eq(RemoteBookshelfEntity::getUserId, userId)
                        .orderByDesc(RemoteBookshelfEntity::getCreatedAt));
        List<RemoteBookshelfItemResponse> items = result.getRecords().stream()
                .map(RemoteBookshelfItemResponse::from).toList();
        return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional
    public RemoteBookshelfItemResponse save(long userId, SaveRemoteBookRequest request) {
        String url = normalizeUrl(request.sourceUrl());
        byte[] hash = hash(url);
        RemoteBookshelfEntity item = find(userId, hash);
        LocalDateTime now = LocalDateTime.now(clock);
        if (item == null) {
            item = new RemoteBookshelfEntity();
            item.setUserId(userId);
            item.setSourceUrlHash(hash);
            item.setCreatedAt(now);
        }
        item.setSourceId(request.sourceId());
        item.setSourceName(request.sourceName().trim());
        item.setSourceUrl(url);
        item.setTitle(request.title().trim());
        item.setAuthor(request.author().trim());
        item.setDescription(request.description());
        item.setCoverUrl(request.coverUrl());
        item.setCategory(request.category());
        item.setLatestChapterTitle(request.latestChapterTitle());
        item.setStatusText(request.statusText());
        item.setImportSupported(request.importSupported());
        item.setUpdatedAt(now);
        if (item.getId() == null) mapper.insert(item); else mapper.updateById(item);
        return RemoteBookshelfItemResponse.from(item);
    }

    @Transactional
    public void remove(long userId, String rawId) {
        mapper.delete(new LambdaQueryWrapper<RemoteBookshelfEntity>()
                .eq(RemoteBookshelfEntity::getUserId, userId)
                .eq(RemoteBookshelfEntity::getId, parseId(rawId)));
    }

    @Transactional
    public void markImported(long userId, String sourceUrl, long bookId) {
        RemoteBookshelfEntity item = find(userId, hash(normalizeUrl(sourceUrl)));
        if (item != null) {
            item.setImportedBookId(bookId);
            item.setUpdatedAt(LocalDateTime.now(clock));
            mapper.updateById(item);
        }
    }

    private RemoteBookshelfEntity find(long userId, byte[] hash) {
        return mapper.selectOne(new LambdaQueryWrapper<RemoteBookshelfEntity>()
                .eq(RemoteBookshelfEntity::getUserId, userId)
                .eq(RemoteBookshelfEntity::getSourceUrlHash, hash).last("LIMIT 1"));
    }

    private String normalizeUrl(String raw) {
        URI uri = URI.create(raw.trim()).normalize();
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 40008, "INVALID_SOURCE_URL", "来源地址不合法");
        }
        return uri.toString();
    }

    private byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private long parseId(String value) {
        try { return Long.parseUnsignedLong(value); }
        catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 40007, "INVALID_QUERY", "资源 ID 格式不正确");
        }
    }
}
