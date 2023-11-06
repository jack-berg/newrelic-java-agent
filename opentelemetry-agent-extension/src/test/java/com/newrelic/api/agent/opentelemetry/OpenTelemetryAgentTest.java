package com.newrelic.api.agent.opentelemetry;

import com.newrelic.api.agent.TraceMetadata;
import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

class OpenTelemetryAgentTest {

    @RegisterExtension
    static OpenTelemetryExtension openTelemetry = OpenTelemetryExtension.create();

    @BeforeEach
    void setup() {
        OpenTelemetryNewRelic.resetForTest();
        OpenTelemetryNewRelic.install(openTelemetry.getOpenTelemetry());
    }

    @AfterEach
    void cleanup() {
        OpenTelemetryNewRelic.resetForTest();
    }

    @Test
    void getLinkingMetadata() {
        Span span = openTelemetry.getOpenTelemetry().getTracer("scopeName").spanBuilder("spanName").startSpan();
        try (Scope unused = span.makeCurrent()) {
            assertThat(OpenTelemetryNewRelic.getAgent().getLinkingMetadata())
                    .hasEntrySatisfying("trace.id", value -> assertThat(value).isNotEmpty())
                    .hasEntrySatisfying("span.id", value -> assertThat(value).isNotEmpty());
        } finally {
            span.end();
        }
        assertThat(OpenTelemetryNewRelic.getAgent().getLinkingMetadata())
                .isEmpty();
    }

    @Test
    void getTraceMetadata() {
        Span span = openTelemetry.getOpenTelemetry().getTracer("scopeName").spanBuilder("spanName").startSpan();
        try (Scope unused = span.makeCurrent()) {
            TraceMetadata traceMetadata = OpenTelemetryNewRelic.getAgent().getTraceMetadata();
            assertThat(traceMetadata.getTraceId()).isNotEmpty();
            assertThat(traceMetadata.getSpanId()).isNotEmpty();
            assertThat(traceMetadata.isSampled()).isTrue();
        } finally {
            span.end();
        }
        TraceMetadata traceMetadata = OpenTelemetryNewRelic.getAgent().getTraceMetadata();
        assertThat(traceMetadata.getTraceId()).isEqualTo(TraceId.getInvalid());
        assertThat(traceMetadata.getSpanId()).isEqualTo(SpanId.getInvalid());
        assertThat(traceMetadata.isSampled()).isFalse();
    }

}
