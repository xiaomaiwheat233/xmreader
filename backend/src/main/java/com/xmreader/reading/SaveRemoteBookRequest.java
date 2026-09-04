package com.xmreader.reading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveRemoteBookRequest(
        int sourceId,
        @NotBlank @Size(max = 100) String sourceName,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://.+") String sourceUrl,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 128) String author,
        @Size(max = 16000) String description,
        @Size(max = 2048) String coverUrl,
        @Size(max = 64) String category,
        @Size(max = 255) String latestChapterTitle,
        @Size(max = 64) String statusText,
        boolean importSupported) {
}
