package com.syncturtle.common.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;

@DisplayName("TraceContextPropagator")
class TraceContextPropagatorTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    private final TraceContextPropagator propagator = new TraceContextPropagator(
            OpenTelemetry.propagating(
                    ContextPropagators.create(W3CTraceContextPropagator.getInstance())));

    @Nested
    @DisplayName("capture()")
    class CaptureTests {

        @Test
        @DisplayName("serializes the current valid W3C trace context")
        void serializesCurrentValidW3cTraceContext() {
            SpanContext spanContext = SpanContext.create(
                    TRACE_ID,
                    SPAN_ID,
                    TraceFlags.getSampled(),
                    TraceState.getDefault());

            try (Scope ignored = Context.root().with(Span.wrap(spanContext)).makeCurrent()) {
                PersistedTraceContext captured = propagator.capture();

                assertThat(captured.traceparent())
                        .isEqualTo("00-" + TRACE_ID + "-" + SPAN_ID + "-01");
                assertThat(captured.tracestate()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("runWith(PersistedTraceContext, Runnable)")
    class RunWithTests {

        @Test
        @DisplayName("restores a persisted context as a valid remote parent")
        void restoresPersistedContextAsValidRemoteParent() {
            PersistedTraceContext persisted = new PersistedTraceContext(
                    "00-" + TRACE_ID + "-" + SPAN_ID + "-01",
                    null);
            AtomicReference<SpanContext> restored = new AtomicReference<>();

            propagator.runWith(
                    persisted,
                    () -> restored.set(Span.current().getSpanContext()));

            assertThat(restored.get().isValid()).isTrue();
            assertThat(restored.get().isRemote()).isTrue();
            assertThat(restored.get().getTraceId()).isEqualTo(TRACE_ID);
            assertThat(restored.get().getSpanId()).isEqualTo(SPAN_ID);
        }

        @Test
        @DisplayName("runs without changing context when no traceparent was persisted")
        void runsWithoutChangingContextWhenTraceparentIsMissing() {
            AtomicReference<SpanContext> current = new AtomicReference<>();

            propagator.runWith(
                    new PersistedTraceContext(null, null),
                    () -> current.set(Span.current().getSpanContext()));

            assertThat(current.get().isValid()).isFalse();
        }
    }

}
