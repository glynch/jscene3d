/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;

import java.util.Objects;
import java.util.Optional;

/** Descriptor for one signal output or action input. */
public final class EndpointDescriptor {
    private final String id;
    private final Optional<RegisteredType> payload;
    private final DescriptorPresentation presentation;

    /** Stores one validated endpoint descriptor. */
    private EndpointDescriptor(String id, Optional<RegisteredType> payload, DescriptorPresentation presentation) {
        this.id = requireLocalId(id, "id");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    /**
     * Creates an endpoint that carries no payload.
     *
     * @param id stable local endpoint identifier
     * @param presentation human-readable endpoint metadata
     * @return endpoint without a payload
     */
    public static EndpointDescriptor withoutPayload(String id, DescriptorPresentation presentation) {
        return new EndpointDescriptor(id, Optional.empty(), presentation);
    }

    /**
     * Creates an endpoint carrying one registered payload type.
     *
     * @param id stable local endpoint identifier
     * @param payload registered payload type
     * @param presentation human-readable endpoint metadata
     * @return endpoint with a payload
     */
    public static EndpointDescriptor withPayload(
            String id, RegisteredType payload, DescriptorPresentation presentation) {
        return new EndpointDescriptor(id, Optional.of(payload), presentation);
    }

    /**
     * Returns the stable local endpoint identifier.
     *
     * @return endpoint identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the payload type, or empty for a signal or action without a payload.
     *
     * @return optional payload type
     */
    public Optional<RegisteredType> payload() {
        return payload;
    }

    /**
     * Returns human-readable endpoint metadata.
     *
     * @return presentation metadata
     */
    public DescriptorPresentation presentation() {
        return presentation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof EndpointDescriptor descriptor
                && id.equals(descriptor.id)
                && payload.equals(descriptor.payload)
                && presentation.equals(descriptor.presentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, payload, presentation);
    }

    @Override
    public String toString() {
        return "EndpointDescriptor[id=" + id + ", payload=" + payload + ", presentation=" + presentation + ']';
    }
}
