package com.xmreader.catalog;

import com.xmreader.crawler.ImportedBookResponse;
import com.xmreader.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookUploadService {
    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "(?m)^(\\s*(?:第[0-9零一二三四五六七八九十百千万两]+[章节卷回篇]|序章|楔子|前言|后记|番外)[^\\r\\n]*)$");

    private final ContentSourceMapper sourceMapper;
    private final BookMapper bookMapper;
    private final ChapterMapper chapterMapper;
    private final Clock clock;

    public BookUploadService(ContentSourceMapper sourceMapper, BookMapper bookMapper,
            ChapterMapper chapterMapper, Clock clock) {
        this.sourceMapper = sourceMapper;
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.clock = clock;
    }

    @Transactional
    public ImportedBookResponse upload(long userId, String rawTitle, String rawAuthor, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("请选择要上传的 TXT 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".txt")) {
            throw invalid("当前仅支持 UTF-8 编码的 TXT 小说");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "")
                    .replace("\r\n", "\n").replace('\r', '\n').trim();
        } catch (Exception exception) {
            throw invalid("无法读取上传文件");
        }
        if (content.isBlank()) throw invalid("小说文件内容不能为空");
        String title = value(rawTitle, filename.substring(0, Math.max(0, filename.length() - 4)));
        String author = value(rawAuthor, "未知作者");
        List<Part> parts = split(content);
        LocalDateTime now = LocalDateTime.now(clock);
        ContentSourceEntity source = uploadSource(now);
        String token = UUID.randomUUID().toString();

        BookEntity book = new BookEntity();
        book.setSourceId(source.getId());
        book.setOwnerUserId(userId);
        book.setSourceBookId(token);
        book.setSourceUrl("upload://" + token);
        book.setSourceUrlHash(hash(book.getSourceUrl()));
        book.setTitle(truncate(title, 255));
        book.setAuthor(truncate(author, 128));
        book.setStatus("COMPLETED");
        book.setVisibility("VISIBLE");
        book.setAccessScope("PUBLIC");
        book.setOriginType("UPLOAD");
        book.setWordCount(0L);
        book.setChapterCount(0);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        bookMapper.insert(book);

        long totalWords = 0;
        ChapterEntity latest = null;
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            ChapterEntity chapter = new ChapterEntity();
            chapter.setBookId(book.getId());
            chapter.setChapterIndex(i + 1);
            chapter.setTitle(truncate(part.title(), 255));
            chapter.setContent(part.content());
            chapter.setContentHash(hash(part.content()));
            int words = part.content().replaceAll("\\s", "").codePointCount(0,
                    part.content().replaceAll("\\s", "").length());
            chapter.setWordCount(words);
            chapter.setCreatedAt(now);
            chapter.setUpdatedAt(now);
            chapterMapper.insert(chapter);
            totalWords += words;
            latest = chapter;
        }
        book.setChapterCount(parts.size());
        book.setWordCount(totalWords);
        book.setLatestChapterId(latest == null ? null : latest.getId());
        book.setLatestChapterTitle(latest == null ? null : latest.getTitle());
        bookMapper.updateById(book);
        return new ImportedBookResponse(Long.toUnsignedString(book.getId()), book.getTitle(), parts.size());
    }

    private List<Part> split(String content) {
        Matcher matcher = CHAPTER_HEADING.matcher(content);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group().trim());
        }
        if (starts.isEmpty()) return List.of(new Part("正文", content));
        List<Part> parts = new ArrayList<>();
        String preface = content.substring(0, starts.get(0)).trim();
        if (!preface.isBlank()) parts.add(new Part("前言", preface));
        for (int i = 0; i < starts.size(); i++) {
            int bodyStart = content.indexOf('\n', starts.get(i));
            if (bodyStart < 0) bodyStart = content.length(); else bodyStart++;
            int end = i + 1 < starts.size() ? starts.get(i + 1) : content.length();
            String body = content.substring(bodyStart, end).trim();
            if (!body.isBlank()) parts.add(new Part(titles.get(i), body));
        }
        return parts.isEmpty() ? List.of(new Part("正文", content)) : parts;
    }

    private ContentSourceEntity uploadSource(LocalDateTime now) {
        ContentSourceEntity source = sourceMapper.findBySourceKey("user-upload");
        if (source != null) return source;
        source = new ContentSourceEntity();
        source.setSourceKey("user-upload");
        source.setDisplayName("用户上传");
        source.setBaseUrl("https://xmreader.local/uploads/");
        source.setAdapterType("FIXTURE");
        source.setEnabled(true);
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
        sourceMapper.insert(source);
        return source;
    }

    private byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String truncate(String value, int length) { return value.length() <= length ? value : value.substring(0, length); }
    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, 40009, "INVALID_UPLOAD", message);
    }
    private record Part(String title, String content) { }
}
