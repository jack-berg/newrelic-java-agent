package com.newrelic.api.agent.opentelemetry;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

class OpenTelemetryMetricAggregatorTest {

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
    void recordMetric() {
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().recordMetric("name1", 10.0f);
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().recordMetric("name1", 10.0f);
        OpenTelemetryNewRelic.recordMetric("name2", 10.0f);
        OpenTelemetryNewRelic.recordMetric("name2", 10.0f);

        assertThat(openTelemetry.getMetrics())
                .satisfiesExactlyInAnyOrder(
                        metricData -> assertThat(metricData).hasName("newrelic.timeslice.histogram.value")
                                .hasHistogramSatisfying(histogram -> histogram.hasPointsSatisfying(
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "name1").build())
                                                .hasSum(20.0).hasCount(2),
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "name2").build())
                                                .hasSum(20.0)
                                                .hasCount(2)
                                ))
                );
    }

    @Test
    void recordResponseTimeMetric() {
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().recordResponseTimeMetric("test-response-time1", 10, 5, TimeUnit.MILLISECONDS);
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().recordResponseTimeMetric("test-response-time1", 10);
        OpenTelemetryNewRelic.recordResponseTimeMetric("test-response-time2", 10);
        OpenTelemetryNewRelic.recordResponseTimeMetric("test-response-time2", 10);

        assertThat(openTelemetry.getMetrics())
                .satisfiesExactlyInAnyOrder(
                        metricData -> assertThat(metricData).hasName("newrelic.timeslice.histogram.value")
                                .hasHistogramSatisfying(histogram -> histogram.hasPointsSatisfying(
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "test-response-time1").build())
                                                .hasSum(0.02).hasCount(2),
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "test-response-time2").build())
                                                .hasSum(0.02)
                                                .hasCount(2)
                                ))
                );
    }

    @Test
    void incrementCounter() {
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().incrementCounter("test-counter1");
        OpenTelemetryNewRelic.getAgent().getMetricAggregator().incrementCounter("test-counter1", 2);
        OpenTelemetryNewRelic.incrementCounter("test-counter2");
        OpenTelemetryNewRelic.incrementCounter("test-counter2", 2);

        assertThat(openTelemetry.getMetrics())
                .satisfiesExactlyInAnyOrder(
                        metricData -> assertThat(metricData).hasName("newrelic.timeslice.counter.value")
                                .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "test-counter1").build())
                                                .hasValue(3.0),
                                        point -> point.hasAttributes(Attributes.builder().put("newrelic.timeslice_name", "test-counter2").build()).hasValue(3.0)
                                ))
                );
    }

}