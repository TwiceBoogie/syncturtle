package com.syncturtle.common.cache.template;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.cache.property.RedisKeyProperties;

public final class RedisKeyBuilder {

    private final RedisKeyProperties properties;

    public RedisKeyBuilder(RedisKeyProperties properties) {
        Assert.notNull(properties, "properties is required");

        this.properties = properties;
    }

    public String key(String namespace, String owner, String resource, String... details) {
        StringBuilder sb = new StringBuilder(128);

        sb.append(properties.getKeyPrefix())
                .append(requireSegment(namespace, "namespace"))
                .append(':')
                .append(requireSegment(owner, "owner"))
                .append(':')
                .append(requireSegment(resource, "resource"));

        if (details != null) {
            for (String detail : details) {
                sb.append(':').append(requireSegment(detail, "detail"));
            }
        }

        return sb.toString();
    }

    public String authKey(String owner, String resource, String... details) {
        return key("auth", owner, resource, details);
    }

    public String configKey(String owner, String resource, String... details) {
        return key("cfg", owner, resource, details);
    }

    public String projectionKey(String owner, String resource, String... details) {
        return key("proj", owner, resource, details);
    }

    public String lockKey(String owner, String resource, String... details) {
        return key("lock", owner, resource, details);
    }

    public String idempotencyKey(String owner, String resource, String... details) {
        return key("idem", owner, resource, details);
    }

    public String rateLimitKey(String owner, String resource, String... details) {
        return key("rl", owner, resource, details);
    }

    private static String requireSegment(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Redis key " + name + " is required");
        }

        String trimmed = value.trim();

        if (trimmed.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Redis key " + name + " must not contain ':'");
        }

        if (trimmed.contains(" ")) {
            throw new IllegalArgumentException("Redis key " + name + " must not contain spaces");
        }

        return trimmed;
    }

}
