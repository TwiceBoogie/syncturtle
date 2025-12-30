package com.syncturtle.common.spring.cache.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CachedHttpResponse {
    private int status;
    private String body;
}
