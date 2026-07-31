package com.syncturtle.services.file.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class PresignedPostResponse {

    private final String url;
    private final Map<String, String> fields;

    public PresignedPostResponse(String url, Map<String, String> fields) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }

        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields are required");
        }

        this.url = url.trim();
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

}
