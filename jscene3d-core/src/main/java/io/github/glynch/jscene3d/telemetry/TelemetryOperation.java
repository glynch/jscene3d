/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.telemetry;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Thread-safe timed operation which may create explicit children across execution-thread boundaries. */
public final class TelemetryOperation implements AutoCloseable {
    private static final TelemetryOperation DISABLED = new TelemetryOperation();

    private final @Nullable Telemetry telemetry;
    private final long traceId;
    private final long operationId;
    private final long parentOperationId;
    private final String name;
    private final Map<String, String> attributes;
    private final long startedAt;

    private boolean closed;
    private long finishedAt;
    private @Nullable String failureType;
    private @Nullable String failureMessage;

    /** Creates the shared disabled operation. */
    private TelemetryOperation() {
        telemetry = null;
        traceId = 0L;
        operationId = 0L;
        parentOperationId = 0L;
        name = "disabled";
        attributes = Map.of();
        startedAt = 0L;
    }

    /** Creates one enabled operation owned by a recorder. */
    private TelemetryOperation(
            Telemetry telemetry,
            long traceId,
            long operationId,
            long parentOperationId,
            String name,
            Map<String, String> attributes,
            long startedAt) {
        this.telemetry = telemetry;
        this.traceId = traceId;
        this.operationId = operationId;
        this.parentOperationId = parentOperationId;
        this.name = name;
        this.attributes = attributes;
        this.startedAt = startedAt;
    }

    /** Returns the shared no-op implementation. */
    static TelemetryOperation disabled() {
        return DISABLED;
    }

    /** Creates one enabled operation from validated recorder state. */
    static TelemetryOperation start(
            Telemetry telemetry,
            long traceId,
            long operationId,
            long parentOperationId,
            String name,
            Map<String, String> attributes,
            long startedAt) {
        return new TelemetryOperation(telemetry, traceId, operationId, parentOperationId, name, attributes, startedAt);
    }

    /**
     * Begins an explicitly related child which may be completed on another thread.
     *
     * @param childName stable semantic name of the child operation
     * @param childAttributes low-cardinality context copied when the child begins
     * @return a child operation which publishes when closed
     */
    public synchronized TelemetryOperation begin(String childName, Map<String, String> childAttributes) {
        if (telemetry == null) {
            return DISABLED;
        }
        requireOpen();
        return telemetry.beginChild(this, childName, childAttributes);
    }

    /**
     * Measures one value-returning child operation and preserves its original failure behavior.
     *
     * @param <T> result type of the measured action
     * @param childName stable semantic name of the child operation
     * @param childAttributes low-cardinality context copied when the child begins
     * @param action work to execute inside the child operation
     * @return the action result
     */
    public <T> T measure(String childName, Map<String, String> childAttributes, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (TelemetryOperation child = begin(childName, childAttributes)) {
            try {
                return action.get();
            } catch (RuntimeException failure) {
                child.fail(failure);
                throw failure;
            }
        }
    }

    /**
     * Marks this operation failed while retaining only safe textual failure metadata.
     *
     * @param failure failure whose type and message should be captured
     */
    public synchronized void fail(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (telemetry == null) {
            return;
        }
        requireOpen();
        if (failureType == null) {
            failureType = failure.getClass().getName();
            failureMessage = Objects.requireNonNullElse(failure.getMessage(), failure.toString());
        }
    }

    /**
     * Marks this operation failed for a non-exceptional domain outcome.
     *
     * @param message stable explanation of the failed outcome
     */
    public synchronized void fail(String message) {
        Objects.requireNonNull(message, "message");
        if (telemetry == null) {
            return;
        }
        requireOpen();
        if (failureMessage == null) {
            failureMessage = message;
        }
    }

    /**
     * Returns elapsed monotonic time, fixed at the completed duration after close.
     *
     * @return elapsed time since this operation began
     */
    public synchronized Duration elapsed() {
        if (telemetry == null) {
            return Duration.ZERO;
        }
        long end = closed ? finishedAt : telemetry.now();
        return durationBetween(startedAt, end);
    }

    /** Completes and publishes this operation at most once. */
    @Override
    public void close() {
        @Nullable TelemetryMeasurement measurement;
        synchronized (this) {
            if (telemetry == null || closed) {
                return;
            }
            finishedAt = telemetry.now();
            closed = true;
            OptionalLong parent = parentOperationId == 0L ? OptionalLong.empty() : OptionalLong.of(parentOperationId);
            TelemetryMeasurement.Outcome outcome = failureMessage == null
                    ? TelemetryMeasurement.Outcome.SUCCEEDED
                    : TelemetryMeasurement.Outcome.FAILED;
            TelemetryMeasurement.Identity identity =
                    new TelemetryMeasurement.Identity(traceId, operationId, parent, name);
            measurement = new TelemetryMeasurement(
                    identity,
                    durationBetween(startedAt, finishedAt),
                    attributes,
                    outcome,
                    Optional.ofNullable(failureType),
                    Optional.ofNullable(failureMessage));
        }
        telemetry.publish(measurement);
    }

    /** Returns the owning trace identity to the recorder. */
    long traceId() {
        return traceId;
    }

    /** Returns this operation identity to the recorder. */
    long operationId() {
        return operationId;
    }

    /** Rejects children and state mutation after publication. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("telemetry operation is already closed");
        }
    }

    /** Converts a monotonic timestamp difference into a non-negative duration. */
    private static Duration durationBetween(long start, long end) {
        return Duration.ofNanos(Math.max(0L, end - start));
    }
}
