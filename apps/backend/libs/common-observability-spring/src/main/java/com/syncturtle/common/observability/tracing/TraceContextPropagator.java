package com.syncturtle.common.observability.tracing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.util.Assert;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;

public final class TraceContextPropagator {

    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";

    private static final TextMapGetter<PersistedTraceContext> GETTER = new TextMapGetter<>() {

        @Override
        public Iterable<String> keys(PersistedTraceContext carrier) {
            return List.of(TRACEPARENT, TRACESTATE);
        }

        @Override
        public String get(PersistedTraceContext carrier, String key) {
            if (carrier == null || key == null) {
                return null;
            }

            if (TRACEPARENT.equalsIgnoreCase(key)) {
                return carrier.traceparent();
            }

            if (TRACESTATE.equalsIgnoreCase(key)) {
                return carrier.tracestate();
            }

            return null;
        }
    };

    private final TextMapPropagator propagator;

    public TraceContextPropagator(OpenTelemetry openTelemetry) {
        Assert.notNull(openTelemetry, "openTelemetry is required");

        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    public PersistedTraceContext capture() {
        Map<String, String> carrier = new LinkedHashMap<>();

        propagator.inject(
                Context.current(),
                carrier,
                (map, key, value) -> map.put(key, value));

        return new PersistedTraceContext(
                carrier.get(TRACEPARENT),
                carrier.get(TRACESTATE));
    }

    public void runWith(PersistedTraceContext persistedContext, Runnable action) {
        Assert.notNull(action, "action is required");

        callWith(persistedContext, () -> {
            action.run();
            return null;
        });
    }

    public <T> T callWith(PersistedTraceContext persistedContext, Supplier<T> action) {
        Assert.notNull(action, "action is required");

        if (persistedContext == null || !persistedContext.isPresent()) {
            return action.get();
        }

        Context extracted = propagator.extract(Context.root(), persistedContext, GETTER);

        try (Scope ignored = extracted.makeCurrent()) {
            return action.get();
        }
    }

}
