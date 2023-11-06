package com.newrelic.api.agent.opentelemetry;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

class OpenTelemetryErrorApiTest {

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
    void operationArgs(Runnable runnable, Consumer<EventData> eventConsumer) {
        Span span = openTelemetry.getOpenTelemetry().getTracer("scopeName").spanBuilder("spanName").startSpan();
        try (Scope unused = span.makeCurrent()) {
            runnable.run();
        } finally {
            span.end();
        }

        assertThat(openTelemetry.getSpans())
                .satisfiesExactly(spanData -> assertThat(spanData.getEvents()).satisfiesExactly(eventConsumer));
    }

    private static Stream<Arguments> operationArgs() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable("error"), Collections.singletonMap("key", "value")),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.stringKey("key"), "value"),
                                        equalTo(AttributeKey.booleanKey("expected.error"), false),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "java.lang.Throwable"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable("error")),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.booleanKey("expected.error"), false),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "java.lang.Throwable"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError("error", Collections.singletonMap("key", "value")),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.stringKey("key"), "value"),
                                        equalTo(AttributeKey.booleanKey("expected.error"), false),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "com.newrelic.opentelemetry.OpenTelemetryErrorApi.ReportedError"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError("error"),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.booleanKey("expected.error"), false),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "com.newrelic.opentelemetry.OpenTelemetryErrorApi.ReportedError"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable("error"), Collections.singletonMap("key", "value"), true),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.stringKey("key"), "value"),
                                        equalTo(AttributeKey.booleanKey("expected.error"), true),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "java.lang.Throwable"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable("error"), true),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.booleanKey("expected.error"), true),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "java.lang.Throwable"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError("error", Collections.singletonMap("key", "value"), true),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.stringKey("key"), "value"),
                                        equalTo(AttributeKey.booleanKey("expected.error"), true),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "com.newrelic.opentelemetry.OpenTelemetryErrorApi.ReportedError"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                ))),
                Arguments.of(
                        (Runnable) () -> OpenTelemetryNewRelic.noticeError("error", true),
                        eventAssert(event -> assertThat(event)
                                .hasName("exception")
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.booleanKey("expected.error"), true),
                                        equalTo(AttributeKey.stringKey("exception.message"), "error"),
                                        equalTo(AttributeKey.stringKey("exception.type"), "com.newrelic.opentelemetry.OpenTelemetryErrorApi.ReportedError"),
                                        satisfies(AttributeKey.stringKey("exception.stacktrace"), value -> assertThat(value).isNotNull())
                                )))
        );
    }

    private static Consumer<EventData> eventAssert(Consumer<EventData> eventConsumer) {
        return eventConsumer;
    }

}
