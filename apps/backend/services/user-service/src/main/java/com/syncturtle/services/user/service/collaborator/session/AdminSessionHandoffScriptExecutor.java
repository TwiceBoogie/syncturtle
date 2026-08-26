package com.syncturtle.services.user.service.collaborator.session;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

public final class AdminSessionHandoffScriptExecutor {

    public static final String SCRIPT_PATH = "redis/admin-session-handoff.lua";

    private final StringRedisTemplate redis;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> script;

    public AdminSessionHandoffScriptExecutor(StringRedisTemplate redis) {
        Assert.notNull(redis, "redis is required");

        this.redis = redis;
        this.script = RedisScript.of(new ClassPathResource(SCRIPT_PATH), List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> execute(List<String> keys, List<String> arguments) {
        Assert.notEmpty(keys, "script keys are required");
        Assert.notNull(arguments, "script arguments are required");

        List<String> result = redis.execute(script, keys, arguments.toArray());
        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("admin session handoff script returned no result");
        }
        return result;
    }

}
