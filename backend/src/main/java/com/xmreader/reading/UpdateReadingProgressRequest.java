package com.xmreader.reading;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateReadingProgressRequest(
        @NotBlank String bookId,
        @NotBlank String chapterId,
        @NotNull @Min(0) Integer position,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal progressPercent) {
}
