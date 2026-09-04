package com.xmreader.crawler;

import com.xmreader.reading.RemoteBookshelfService;
import com.xmreader.shared.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ImportTaskManager {
    private final CrawlerGateway gateway;
    private final CrawlerCatalogWriter writer;
    private final RemoteBookshelfService remoteBookshelfService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public ImportTaskManager(
            CrawlerGateway gateway,
            CrawlerCatalogWriter writer,
            RemoteBookshelfService remoteBookshelfService) {
        this.gateway = gateway;
        this.writer = writer;
        this.remoteBookshelfService = remoteBookshelfService;
    }

    public ImportTaskResponse start(long userId, String sourceUrl) {
        String id = UUID.randomUUID().toString();
        Task task = new Task(userId, sourceUrl.trim());
        tasks.put(id, task);
        executor.submit(() -> run(id, task));
        return response(id, task);
    }

    public ImportTaskResponse get(long userId, String taskId) {
        Task task = tasks.get(taskId);
        if (task == null || task.userId != userId) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 40402, "IMPORT_TASK_NOT_FOUND", "导入任务不存在");
        }
        return response(taskId, task);
    }

    private void run(String id, Task task) {
        task.status = "RUNNING";
        try {
            CrawledBook crawled = gateway.fetchBook(task.sourceUrl);
            ImportedBookResponse imported = writer.save(crawled, task.userId);
            task.result = imported;
            remoteBookshelfService.markImported(task.userId, task.sourceUrl,
                    Long.parseUnsignedLong(imported.bookId()));
            task.status = "COMPLETED";
        } catch (Exception exception) {
            task.error = exception.getMessage() == null ? "导入失败" : exception.getMessage();
            task.status = "FAILED";
        }
    }

    private ImportTaskResponse response(String id, Task task) {
        ImportedBookResponse result = task.result;
        return new ImportTaskResponse(id, task.status,
                result == null ? null : result.bookId(), result == null ? null : result.title(),
                result == null ? null : result.importedChapterCount(), task.error);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class Task {
        private final long userId;
        private final String sourceUrl;
        private volatile String status = "QUEUED";
        private volatile ImportedBookResponse result;
        private volatile String error;

        private Task(long userId, String sourceUrl) {
            this.userId = userId;
            this.sourceUrl = sourceUrl;
        }
    }
}
