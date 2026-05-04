package com.syncturtle.platform.gateway.support;

import java.time.Duration;

import com.syncturtle.platform.gateway.enums.SessionType;

import reactor.core.publisher.Mono;

public interface SessionStore {
    Mono<SessionRecord> find(SessionType type, String sessionId);

    Mono<Void> save(SessionType type, String sessionId, SessionRecord record, Duration absoluteRemainingTtl);

    Mono<Void> delete(SessionType type, String sessionId);
}
