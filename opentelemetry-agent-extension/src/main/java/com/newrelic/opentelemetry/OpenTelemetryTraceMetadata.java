package com.newrelic.opentelemetry;

import com.newrelic.api.agent.TraceMetadata;
import io.opentelemetry.api.trace.Span;

final class OpenTelemetryTraceMetadata implements TraceMetadata {
    private final Span span;

    OpenTelemetryTraceMetadata(Span span) {
        this.span = span;
    }

    @Override
    public String getTraceId() {
        return span.getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId() {
        return span.getSpanContext().getSpanId();
    }

    @Override
    public boolean isSampled() {
        return span.getSpanContext().isSampled();
    }
}
