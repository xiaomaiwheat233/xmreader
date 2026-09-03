package com.xmreader.catalog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chapters")
public class ChapterEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long bookId;
    private Integer chapterIndex;
    private String title;
    private String content;
    private byte[] contentHash;
    private String sourceUrl;
    private byte[] sourceUrlHash;
    private Integer wordCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Integer getChapterIndex() { return chapterIndex; }
    public void setChapterIndex(Integer chapterIndex) { this.chapterIndex = chapterIndex; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public byte[] getContentHash() { return contentHash; }
    public void setContentHash(byte[] contentHash) { this.contentHash = contentHash; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public byte[] getSourceUrlHash() { return sourceUrlHash; }
    public void setSourceUrlHash(byte[] sourceUrlHash) { this.sourceUrlHash = sourceUrlHash; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
