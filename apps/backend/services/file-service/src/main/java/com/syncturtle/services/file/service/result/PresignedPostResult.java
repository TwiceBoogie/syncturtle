package com.syncturtle.services.file.service.result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class PresignedPostResult {

    private final String url;
    private final Map<String, String> fields;

    public PresignedPostResult(String url, Map<String, String> fields) {
        Assert.hasText(url, "url is required");
        Assert.notEmpty(fields, "fields are required");

        this.url = url.trim();
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

}
