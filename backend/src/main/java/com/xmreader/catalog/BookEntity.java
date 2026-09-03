package com.xmreader.catalog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("books")
public class BookEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sourceId;
    private String sourceBookId;
    private String sourceUrl;
    private byte[] sourceUrlHash;
    private String title;
    private String author;
    private String coverUrl;
    private String description;
    private String category;
    private String status;
    private String visibility;
    private Long wordCount;
    private Integer chapterCount;
    private Long latestChapterId;
    private String latestChapterTitle;
    private LocalDateTime lastCrawledAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceBookId() { return sourceBookId; }
    public void setSourceBookId(String sourceBookId) { this.sourceBookId = sourceBookId; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public byte[] getSourceUrlHash() { return sourceUrlHash; }
    public void setSourceUrlHash(byte[] sourceUrlHash) { this.sourceUrlHash = sourceUrlHash; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Long getWordCount() { return wordCount; }
    public void setWordCount(Long wordCount) { this.wordCount = wordCount; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public Long getLatestChapterId() { return latestChapterId; }
    public void setLatestChapterId(Long latestChapterId) { this.latestChapterId = latestChapterId; }
    public String getLatestChapterTitle() { return latestChapterTitle; }
    public void setLatestChapterTitle(String latestChapterTitle) { this.latestChapterTitle = latestChapterTitle; }
    public LocalDateTime getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(LocalDateTime lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
