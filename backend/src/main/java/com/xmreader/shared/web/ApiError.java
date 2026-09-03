package com.xmreader.shared.web;

import java.util.Map;

public record ApiError(String type, Map<String, String> fields) {

    public static ApiError of(String type) {
        return new ApiError(type, Map.of());
    }
}
