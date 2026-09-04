package com.xmreader.crawler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ImportBookRequest(
        @NotBlank(message = "来源地址不能为空")
        @Size(max = 2048, message = "来源地址过长")
        @Pattern(regexp = "^https?://.+", message = "来源地址必须是 HTTP(S) URL")
        String sourceUrl) {
}
