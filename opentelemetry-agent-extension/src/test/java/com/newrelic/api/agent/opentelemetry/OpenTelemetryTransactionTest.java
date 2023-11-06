package com.newrelic.api.agent.opentelemetry;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

class OpenTelemetryTransactionTest {

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

    @ParameterizedTest
    @MethodSource("operationArgs")
    void operations(Runnable runnable, Consumer<SpanData> spanConsumer) {
        runnable.run();

        assertThat(openTelemetry.getSpans())
                .satisfiesExactly(spanConsumer);
    }

    private static Stream<Arguments> operationArgs() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("double_key2", 2.2f);
        attributes.put("long_key2", 2);
        attributes.put("bool_key2", false);
        attributes.put("string_key2", "value2");
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> {
                            Segment segment = OpenTelemetryNewRelic.getAgent().getTransaction().startSegment("spanName");
                            segment.addCustomAttribute("double_key1", 1.1d);
                            segment.addCustomAttribute("long_key1", 1L);
                            segment.addCustomAttribute("string_key1", "value1");
                            segment.addCustomAttribute("bool_key1", true);
                            segment.addCustomAttributes(attributes);
                            segment.end();
                        },
                        spanAssert(span -> assertThat(span)
                                .hasName("spanName")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.doubleKey("double_key1"), 1.1),
                                        equalTo(AttributeKey.longKey("long_key1"), 1),
                                        equalTo(AttributeKey.stringKey("string_key1"), "value1"),
                                        equalTo(AttributeKey.booleanKey("bool_key1"), true),
                                        satisfies(AttributeKey.doubleKey("double_key2"), value -> value.isCloseTo(2.2, Offset.offset(0.01))),
                                        equalTo(AttributeKey.longKey("long_key2"), 2),
                                        equalTo(AttributeKey.stringKey("string_key2"), "value2"),
                                        equalTo(AttributeKey.booleanKey("bool_key2"), false)
                                ))),
                Arguments.of(
                        (Runnable) () -> {
                            Segment segment = OpenTelemetryNewRelic.getAgent().getTransaction().startSegment("category", "spanName");
                            segment.addCustomAttribute("double_key1", 1.1d);
                            segment.addCustomAttribute("long_key1", 1L);
                            segment.addCustomAttribute("string_key1", "value1");
                            segment.addCustomAttribute("bool_key1", true);
                            segment.addCustomAttributes(attributes);
                            segment.endAsync();
                        },
                        spanAssert(span -> assertThat(span)
                                .hasName("category/spanName")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.doubleKey("double_key1"), 1.1),
                                        equalTo(AttributeKey.longKey("long_key1"), 1),
                                        equalTo(AttributeKey.stringKey("string_key1"), "value1"),
                                        equalTo(AttributeKey.booleanKey("bool_key1"), true),
                                        satisfies(AttributeKey.doubleKey("double_key2"), value -> value.isCloseTo(2.2, Offset.offset(0.01))),
                                        equalTo(AttributeKey.longKey("long_key2"), 2),
                                        equalTo(AttributeKey.stringKey("string_key2"), "value2"),
                                        equalTo(AttributeKey.booleanKey("bool_key2"), false)
                                ))),
                Arguments.of(
                        (Runnable) () -> {
                            Span span = openTelemetry.getOpenTelemetry().getTracer("tracer").spanBuilder("spanName").startSpan();
                            try (Scope unused = span.makeCurrent()) {
                                OpenTelemetryNewRelic.setTransactionName("category", "newName");
                            } finally {
                                span.end();
                            }
                        },
                        spanAssert(span -> assertThat(span)
                                .hasName("category/newName"))),
                Arguments.of(
                        (Runnable) () -> {
                            Span span = openTelemetry.getOpenTelemetry().getTracer("tracer").spanBuilder("spanName").startSpan();
                            try (Scope unused = span.makeCurrent()) {
                                OpenTelemetryNewRelic.getAgent()
                                        .getTransaction()
                                        .setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "category", "newName");
                            } finally {
                                span.end();
                            }
                        },
                        spanAssert(span -> assertThat(span)
                                .hasName("category/newName"))),
                Arguments.of(
                        (Runnable) () -> {
                            Span span = openTelemetry.getOpenTelemetry().getTracer("tracer").spanBuilder("spanName").startSpan();
                            try (Scope unused = span.makeCurrent()) {
                                OpenTelemetryNewRelic.getAgent()
                                        .getTransaction()
                                        .setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, null, "part1", "part2");
                            } finally {
                                span.end();
                            }
                        },
                        spanAssert(span -> assertThat(span)
                                .hasName("Java/part1/part2")))
        );
    }

    private static Consumer<SpanData> spanAssert(Consumer<SpanData> spanConsumer) {
        return spanConsumer;
    }

}
