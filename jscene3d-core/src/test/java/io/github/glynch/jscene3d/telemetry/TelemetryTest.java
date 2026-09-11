/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Exercises hierarchical timing, failure capture, and observer isolation. */
final class TelemetryTest {
    /** Publishes immutable nested measurements with deterministic monotonic durations. */
    @Test
    void recordsNestedOperations() {
        AtomicLong clock = new AtomicLong(100L);
        List<TelemetryMeasurement> measurements = new ArrayList<>();
        Telemetry telemetry = Telemetry.recording(measurements::add, clock::get);

        try (TelemetryOperation root = telemetry.begin("project.open", Map.of("project", "garden"))) {
            clock.addAndGet(10L);
            try (TelemetryOperation child = root.begin("project.load", Map.of())) {
                clock.addAndGet(20L);
            }
            clock.addAndGet(5L);
        }

        assertThat(measurements).hasSize(2);
        TelemetryMeasurement child = measurements.getFirst();
        TelemetryMeasurement root = measurements.getLast();
        assertThat(child.name()).isEqualTo("project.load");
        assertThat(child.duration()).isEqualTo(Duration.ofNanos(20L));
        assertThat(child.traceId()).isEqualTo(root.traceId());
        assertThat(child.parentOperationId()).hasValue(root.operationId());
        assertThat(root.duration()).isEqualTo(Duration.ofNanos(35L));
        assertThat(root.attributes()).containsEntry("project", "garden");
        assertThat(root.outcome()).isEqualTo(TelemetryMeasurement.Outcome.SUCCEEDED);
    }

    /** Marks a measured child failed and rethrows the original exception unchanged. */
    @Test
    void preservesMeasuredFailure() {
        List<TelemetryMeasurement> measurements = new ArrayList<>();
        Telemetry telemetry = Telemetry.recording(measurements::add);
        IllegalStateException failure = new IllegalStateException("unavailable");
        Supplier<String> action = () -> {
            throw failure;
        };
        Map<String, String> attributes = Map.of();

        try (TelemetryOperation root = telemetry.begin("project.open", Map.of())) {
            assertThatThrownBy(() -> root.measure("project.load", attributes, action))
                    .isSameAs(failure);
        }

        assertThat(measurements.getFirst().outcome()).isEqualTo(TelemetryMeasurement.Outcome.FAILED);
        assertThat(measurements.getFirst().failureType()).contains(IllegalStateException.class.getName());
        assertThat(measurements.getFirst().failureMessage()).contains("unavailable");
    }

    /** Prevents observer defects from changing the result of an instrumented operation. */
    @Test
    void isolatesObserverFailure() {
        IllegalArgumentException failure = new IllegalArgumentException("observer defect");
        List<RuntimeException> rejected = new ArrayList<>();
        Telemetry telemetry = Telemetry.recording(
                measurement -> {
                    throw failure;
                },
                System::nanoTime,
                rejected::add);

        assertThatCode(() -> {
                    try (TelemetryOperation ignored = telemetry.begin("project.open", Map.of())) {
                        // Closing the scope exercises publication.
                    }
                })
                .doesNotThrowAnyException();
        assertThat(rejected).singleElement().isSameAs(failure);
    }

    /** Executes measured work normally when telemetry is disabled. */
    @Test
    void disablesRecordingWithoutChangingControlFlow() {
        TelemetryOperation root = Telemetry.disabled().begin("project.open", Map.of());

        String result = root.measure("project.load", Map.of(), () -> "loaded");
        root.close();

        assertThat(result).isEqualTo("loaded");
        assertThat(root.elapsed()).isZero();
    }
}
