import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.nr.builder.publish.PublishConfig
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.tasks.Jar

plugins {
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

PublishConfig.config(
        project,
        "New Relic OpenTelemetry Java Agent Extension",
        "Extension for OpenTelemetry Java Agent") {
    artifactId = "newrelic-opentelemetry-agent-extension"
    artifact(shadowJar)

    // Update artifact version to include "-alpha" suffix
    var artifactVersion = version
    val snapshotSuffix = "-SNAPSHOT"
    if (artifactVersion.endsWith(snapshotSuffix)) {
        artifactVersion = artifactVersion.removeSuffix(snapshotSuffix) + "-alpha" + snapshotSuffix
    } else {
        artifactVersion = artifactVersion + "-alpha"
    }
    version = artifactVersion
}

var agent = configurations.create("agent")

var openTelemetryAgentVersion = "1.31.0";
var openTelemetryInstrumentationVersion = "1.31.0-alpha";
var openTelemetrySemConvVersion = "1.22.0-alpha";
var openTelemetryProtoVersion = "1.0.0-alpha";

dependencies {
    implementation(project(":newrelic-api"))

    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${openTelemetryInstrumentationVersion}"))
    compileOnly("io.opentelemetry:opentelemetry-api")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:${openTelemetrySemConvVersion}")

    testImplementation("io.opentelemetry:opentelemetry-api")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("io.github.netmikey.logunit:logunit-jul:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("org.assertj:assertj-core:3.24.2")

    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:${openTelemetryAgentVersion}")
}

tasks.withType<Test> {
    dependsOn(shadowJar)
}

testing {
    suites {
        register<JvmTestSuite>("testAgentExtension") {
            dependencies {
                implementation(project(":newrelic-api"))

                implementation("org.mock-server:mockserver-netty:5.15.0:shaded")
                implementation("io.opentelemetry.proto:opentelemetry-proto:${openTelemetryProtoVersion}")
                implementation("org.assertj:assertj-core:3.24.2")
            }

            targets {
                all {
                    testTask {
                        var extensionJar = shadowJar.archiveFile.get().toString()
                        jvmArgs("-javaagent:${agent.singleFile}",
                                "-Dotel.javaagent.extensions=${extensionJar}",
                                "-Dotel.logs.exporter=otlp",
                                "-Dotel.exporter.otlp.protocol=http/protobuf",
                                "-Dotel.metric.export.interval=500",
                                "-Dotel.bsp.schedule.delay=500",
                                "-Dotel.blrp.schedule.delay=500",
                                "-Dmockserver.disableSystemOut=true" // NOTE: comment out to enable mock server logs for debugging
                        )
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        showStandardStreams = true
    }
}

tasks {
    check {
        dependsOn(testing.suites.getByName("testAgentExtension"))
    }
}
