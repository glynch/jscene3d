/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.telemetry;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/** Immutable result published when one telemetry operation completes. */
public final class TelemetryMeasurement {
    /** Completion state of a measured operation. */
    public enum Outcome {
        /** The operation completed without reporting a failure. */
        SUCCEEDED,
        /** The operation reported a failure or threw while being measured. */
        FAILED
    }

    private final long traceId;
    private final long operationId;
    private final OptionalLong parentOperationId;
    private final String name;
    private final Duration duration;
    private final Map<String, String> attributes;
    private final Outcome outcome;
    private final Optional<String> failureType;
    private final Optional<String> failureMessage;

    /** Stores one validated completed operation without retaining its failure object. */
    TelemetryMeasurement(
            Identity identity,
            Duration duration,
            Map<String, String> attributes,
            Outcome outcome,
            Optional<String> failureType,
            Optional<String> failureMessage) {
        traceId = identity.traceId();
        operationId = identity.operationId();
        parentOperationId = identity.parentOperationId();
        name = identity.name();
        this.duration = duration;
        this.attributes = attributes;
        this.outcome = outcome;
        this.failureType = failureType;
        this.failureMessage = failureMessage;
    }

    /**
     * Returns the identity shared by every operation in the same trace.
     *
     * @return the trace identity
     */
    public long traceId() {
        return traceId;
    }

    /**
     * Returns this operation's identity within its recorder.
     *
     * @return the operation identity
     */
    public long operationId() {
        return operationId;
    }

    /**
     * Returns the parent operation identity, or empty for a trace root.
     *
     * @return the optional parent operation identity
     */
    public OptionalLong parentOperationId() {
        return parentOperationId;
    }

    /**
     * Returns the stable semantic operation name.
     *
     * @return the operation name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the monotonic elapsed duration.
     *
     * @return the elapsed duration
     */
    public Duration duration() {
        return duration;
    }

    /**
     * Returns immutable low-cardinality context attached when the operation began.
     *
     * @return the operation attributes
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Returns whether the operation succeeded or failed.
     *
     * @return the completion outcome
     */
    public Outcome outcome() {
        return outcome;
    }

    /**
     * Returns the Java type of a captured thrown failure, when applicable.
     *
     * @return the optional failure type name
     */
    public Optional<String> failureType() {
        return failureType;
    }

    /**
     * Returns a failure message without retaining the original throwable.
     *
     * @return the optional failure message
     */
    public Optional<String> failureMessage() {
        return failureMessage;
    }

    /** Stable identity and hierarchy fields assigned when an operation begins. */
    record Identity(long traceId, long operationId, OptionalLong parentOperationId, String name) {}
}
