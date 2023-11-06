package com.newrelic.opentelemetry;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.util.Map;

final class OpenTelemetrySegment implements Segment {

    private final Span span;

    private OpenTelemetrySegment(Span span) {
        this.span = span;
    }

    static OpenTelemetrySegment start(Tracer tracer, String segmentName) {
        return new OpenTelemetrySegment(tracer.spanBuilder(segmentName)
                .startSpan());
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        if (value instanceof Double || value instanceof Float) {
            span.setAttribute(key, value.doubleValue());
        } else {
            span.setAttribute(key, value.intValue());
        }
    }

    @Override
    public void addCustomAttribute(String key, String value) {
        span.setAttribute(key, value);
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
        span.setAttribute(key, value);
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
        span.setAllAttributes(OpenTelemetryNewRelic.toAttributes(attributes).build());
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        span.updateName(String.join("/", metricNameParts));
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "reportAsExternal");
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addOutboundRequestHeaders");
    }

    @Override
    public Transaction getTransaction() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "getTransaction");
        return NewRelic.getAgent().getTransaction();
    }

    @Override
    public void ignore() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "ignore");
    }

    @Override
    public void end() {
        span.end();
    }

    @Override
    public void endAsync() {
        span.end();
    }
}
