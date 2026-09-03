package com.xmreader.shared.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, long page, long pageSize, long total) {

    public static <S, T> PageResponse<T> from(IPage<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getRecords().stream().map(mapper).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }
}
