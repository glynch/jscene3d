/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableUniqueIndex;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Safe metadata for one versioned type contributed by an extension. */
public final class RegisteredTypeDescriptor {
    private final RegisteredType type;
    private final RegisteredTypeScope scope;
    private final DescriptorPresentation presentation;
    private final Map<String, PropertyDescriptor> properties;
    private final Map<String, EndpointDescriptor> signals;
    private final Map<String, EndpointDescriptor> actions;
    private final List<String> requiredCapabilities;

    /**
     * Creates one immutable registered-type descriptor.
     *
     * @param type stable registered type and definition version
     * @param scope allowed authoring and runtime scope
     * @param presentation human-readable metadata
     * @param properties project-authored properties
     * @param signals signal outputs
     * @param actions action inputs
     * @param requiredCapabilities runtime capabilities needed by the implementation
     */
    public RegisteredTypeDescriptor(
            RegisteredType type,
            RegisteredTypeScope scope,
            DescriptorPresentation presentation,
            List<PropertyDescriptor> properties,
            List<EndpointDescriptor> signals,
            List<EndpointDescriptor> actions,
            List<String> requiredCapabilities) {
        this.type = Objects.requireNonNull(type, "type");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.properties = immutableUniqueIndex(properties, PropertyDescriptor::id, "properties");
        this.signals = immutableUniqueIndex(signals, EndpointDescriptor::id, "signals");
        this.actions = immutableUniqueIndex(actions, EndpointDescriptor::id, "actions");
        this.requiredCapabilities = List.copyOf(requiredCapabilities);
        for (int index = 0; index < this.requiredCapabilities.size(); index++) {
            requireRegisteredTypeId(this.requiredCapabilities.get(index), "requiredCapabilities[" + index + "]");
        }
    }

    /**
     * Returns the stable registered type and definition version.
     *
     * @return registered type
     */
    public RegisteredType type() {
        return type;
    }

    /**
     * Returns the allowed authoring and runtime scope.
     *
     * @return registered type scope
     */
    public RegisteredTypeScope scope() {
        return scope;
    }

    /**
     * Returns human-readable type metadata.
     *
     * @return presentation metadata
     */
    public DescriptorPresentation presentation() {
        return presentation;
    }

    /**
     * Returns properties by stable local identifier in declaration order.
     *
     * @return immutable property lookup
     */
    public Map<String, PropertyDescriptor> properties() {
        return properties;
    }

    /**
     * Returns signal outputs by stable local identifier in declaration order.
     *
     * @return immutable signal lookup
     */
    public Map<String, EndpointDescriptor> signals() {
        return signals;
    }

    /**
     * Returns action inputs by stable local identifier in declaration order.
     *
     * @return immutable action lookup
     */
    public Map<String, EndpointDescriptor> actions() {
        return actions;
    }

    /**
     * Returns runtime capabilities required by this type.
     *
     * @return immutable required capability identifiers
     */
    public List<String> requiredCapabilities() {
        return requiredCapabilities;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RegisteredTypeDescriptor descriptor
                && type.equals(descriptor.type)
                && scope == descriptor.scope
                && presentation.equals(descriptor.presentation)
                && properties.equals(descriptor.properties)
                && signals.equals(descriptor.signals)
                && actions.equals(descriptor.actions)
                && requiredCapabilities.equals(descriptor.requiredCapabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, scope, presentation, properties, signals, actions, requiredCapabilities);
    }

    @Override
    public String toString() {
        return "RegisteredTypeDescriptor[type=" + type + ", scope=" + scope + ", presentation=" + presentation
                + ", properties=" + properties + ", signals=" + signals + ", actions=" + actions
                + ", requiredCapabilities=" + requiredCapabilities + ']';
    }
}
