package com.xmreader.reading;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("remote_bookshelves")
public class RemoteBookshelfEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer sourceId;
    private String sourceName;
    private String sourceUrl;
    private byte[] sourceUrlHash;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
    private String category;
    private String latestChapterTitle;
    private String statusText;
    private Boolean importSupported;
    private Long importedBookId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public byte[] getSourceUrlHash() { return sourceUrlHash; }
    public void setSourceUrlHash(byte[] sourceUrlHash) { this.sourceUrlHash = sourceUrlHash; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLatestChapterTitle() { return latestChapterTitle; }
    public void setLatestChapterTitle(String latestChapterTitle) { this.latestChapterTitle = latestChapterTitle; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Boolean getImportSupported() { return importSupported; }
    public void setImportSupported(Boolean importSupported) { this.importSupported = importSupported; }
    public Long getImportedBookId() { return importedBookId; }
    public void setImportedBookId(Long importedBookId) { this.importedBookId = importedBookId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
