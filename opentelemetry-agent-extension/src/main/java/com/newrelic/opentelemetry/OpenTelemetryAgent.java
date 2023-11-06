package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.TraceMetadata;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Note this and {@link #create(OpenTelemetry)} are public because they are
 * accessed from package {@code com.newrelic.api.agent} after
 * {@link com.newrelic.api.agent.NewRelic} is rewritten.
 */
public final class OpenTelemetryAgent implements Agent {

    private static final String TRACE_ID = "trace.id";
    private static final String SPAN_ID = "span.id";

    private final OpenTelemetryTransaction openTelemetryTransaction;
    private final OpenTelemetryMetricsAggregator openTelemetryMetricsAggregator;
    private final OpenTelemetryInsights openTelemetryInsights;

    private OpenTelemetryAgent(OpenTelemetry openTelemetry) {
        this.openTelemetryTransaction = OpenTelemetryTransaction.create(openTelemetry);
        this.openTelemetryMetricsAggregator = OpenTelemetryMetricsAggregator.create(openTelemetry);
        this.openTelemetryInsights = OpenTelemetryInsights.create(openTelemetry);
    }

    public static OpenTelemetryAgent create(OpenTelemetry openTelemetry) {
        Objects.requireNonNull(openTelemetry, "openTelemetry must not be null");
        return new OpenTelemetryAgent(openTelemetry);
    }

    @Override
    public TracedMethod getTracedMethod() {
        return OpenTelemetryTracedMethod.getInstance();
    }

    @Override
    public Transaction getTransaction() {
        return openTelemetryTransaction;
    }

    @Override
    public Logger getLogger() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getLogger");
        return NoOpLogger.getInstance();
    }

    @Override
    public Config getConfig() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getConfig");
        return NoOpConfig.getInstance();
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return openTelemetryMetricsAggregator;
    }

    @Override
    public Insights getInsights() {
        return openTelemetryInsights;
    }

    @Override
    public TraceMetadata getTraceMetadata() {
        return new OpenTelemetryTraceMetadata(Span.current());
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        Map<String, String> metadata = new HashMap<>();

        final Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            metadata.put(TRACE_ID, span.getSpanContext().getTraceId());
            metadata.put(SPAN_ID, span.getSpanContext().getSpanId());
        }

        // TODO: any other fields we can populate?

        return Collections.unmodifiableMap(metadata);
    }

    public OpenTelemetryErrorApi getErrorApi() {
        return OpenTelemetryErrorApi.getInstance();
    }

}
