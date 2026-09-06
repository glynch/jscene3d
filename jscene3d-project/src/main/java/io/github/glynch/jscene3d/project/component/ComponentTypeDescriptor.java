/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Authoritative safe metadata for one registered component type.
 *
 * <p>The descriptor is inert: it contains no implementation class name and does not load executable extension code.
 * Its property, endpoint, capability, multiplicity, conflict, spatial, lifecycle, and scheduling declarations are the
 * single source used by authoring validation and runtime factory registration.
 */
public final class ComponentTypeDescriptor {
    private final ComponentType type;
    private final DescriptorPresentation presentation;
    private final Map<PropertyId, PropertyDescriptor> properties;
    private final Map<EndpointId, EndpointDescriptor> signals;
    private final Map<EndpointId, EndpointDescriptor> actions;
    private final Set<CapabilityId> providedCapabilities;
    private final Set<CapabilityId> requiredCapabilities;
    private final Set<AttachmentPointId> attachments;
    private final ComponentMultiplicity multiplicity;
    private final Set<ComponentTypeId> conflicts;
    private final ComponentSpatialDomain spatialDomain;
    private final Set<ComponentLifecycle> lifecycle;
    private final Set<ComponentUpdatePhase> updatePhases;

    /** Copies and validates one completed builder. */
    private ComponentTypeDescriptor(Builder builder) {
        type = builder.type;
        presentation = builder.presentation;
        properties = index(builder.properties, property -> new PropertyId(property.id()), "properties");
        signals = index(builder.signals, signal -> new EndpointId(signal.id()), "signals");
        actions = index(builder.actions, action -> new EndpointId(action.id()), "actions");
        providedCapabilities = immutableSet(builder.providedCapabilities, "providedCapabilities");
        requiredCapabilities = immutableSet(builder.requiredCapabilities, "requiredCapabilities");
        attachments = immutableSet(builder.attachments, "attachments");
        multiplicity = builder.multiplicity;
        conflicts = immutableSet(builder.conflicts, "conflicts");
        spatialDomain = builder.spatialDomain;
        lifecycle = immutableSet(builder.lifecycle, "lifecycle");
        updatePhases = immutableSet(builder.updatePhases, "updatePhases");
        if (!Collections.disjoint(providedCapabilities, requiredCapabilities)) {
            throw new IllegalArgumentException("a component cannot require a capability that it provides");
        }
        if (conflicts.contains(type.id())) {
            throw new IllegalArgumentException("a component type cannot conflict with itself: " + type.id());
        }
        if (!attachments.isEmpty() && spatialDomain == ComponentSpatialDomain.NONE) {
            throw new IllegalArgumentException("attachments require a spatial component domain");
        }
    }

    /**
     * Starts a descriptor builder with its required identity and presentation.
     *
     * @param type exact component type
     * @param presentation human-readable editor and diagnostic metadata
     * @return descriptor builder
     */
    public static Builder builder(ComponentType type, DescriptorPresentation presentation) {
        return new Builder(type, presentation);
    }

    /**
     * Returns the exact component type.
     *
     * @return component type and configuration version
     */
    public ComponentType type() {
        return type;
    }

    /**
     * Returns human-readable editor and diagnostic metadata.
     *
     * @return presentation metadata
     */
    public DescriptorPresentation presentation() {
        return presentation;
    }

    /**
     * Returns property declarations by stable identity in declaration order.
     *
     * @return immutable property index
     */
    public Map<PropertyId, PropertyDescriptor> properties() {
        return properties;
    }

    /**
     * Returns signal declarations by stable identity in declaration order.
     *
     * @return immutable signal index
     */
    public Map<EndpointId, EndpointDescriptor> signals() {
        return signals;
    }

    /**
     * Returns action declarations by stable identity in declaration order.
     *
     * @return immutable action index
     */
    public Map<EndpointId, EndpointDescriptor> actions() {
        return actions;
    }

    /**
     * Returns capabilities supplied to sibling components.
     *
     * @return immutable provided-capability set
     */
    public Set<CapabilityId> providedCapabilities() {
        return providedCapabilities;
    }

    /**
     * Returns capabilities that must resolve to exactly one sibling provider.
     *
     * @return immutable required-capability set
     */
    public Set<CapabilityId> requiredCapabilities() {
        return requiredCapabilities;
    }

    /**
     * Returns named spatial attachments supplied by the component.
     *
     * @return immutable attachment set
     */
    public Set<AttachmentPointId> attachments() {
        return attachments;
    }

    /**
     * Returns the allowed per-entity multiplicity.
     *
     * @return component multiplicity
     */
    public ComponentMultiplicity multiplicity() {
        return multiplicity;
    }

    /**
     * Returns component types that may not coexist on the same entity.
     *
     * @return immutable conflicting-type set
     */
    public Set<ComponentTypeId> conflicts() {
        return conflicts;
    }

    /**
     * Returns the primary spatial authority supplied by this component.
     *
     * @return spatial domain
     */
    public ComponentSpatialDomain spatialDomain() {
        return spatialDomain;
    }

    /**
     * Returns declared semantic lifecycle participation.
     *
     * @return immutable lifecycle-event set
     */
    public Set<ComponentLifecycle> lifecycle() {
        return lifecycle;
    }

    /**
     * Returns declared deterministic update participation.
     *
     * @return immutable update-phase set
     */
    public Set<ComponentUpdatePhase> updatePhases() {
        return updatePhases;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ComponentTypeDescriptor descriptor
                && type.equals(descriptor.type)
                && presentation.equals(descriptor.presentation)
                && properties.equals(descriptor.properties)
                && signals.equals(descriptor.signals)
                && actions.equals(descriptor.actions)
                && providedCapabilities.equals(descriptor.providedCapabilities)
                && requiredCapabilities.equals(descriptor.requiredCapabilities)
                && attachments.equals(descriptor.attachments)
                && multiplicity == descriptor.multiplicity
                && conflicts.equals(descriptor.conflicts)
                && spatialDomain == descriptor.spatialDomain
                && lifecycle.equals(descriptor.lifecycle)
                && updatePhases.equals(descriptor.updatePhases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                presentation,
                properties,
                signals,
                actions,
                providedCapabilities,
                requiredCapabilities,
                attachments,
                multiplicity,
                conflicts,
                spatialDomain,
                lifecycle,
                updatePhases);
    }

    @Override
    public String toString() {
        return "ComponentTypeDescriptor[type=" + type + ", presentation=" + presentation + ", properties="
                + properties + ", signals=" + signals + ", actions=" + actions + ", providedCapabilities="
                + providedCapabilities + ", requiredCapabilities=" + requiredCapabilities + ", attachments="
                + attachments + ", multiplicity=" + multiplicity + ", conflicts=" + conflicts + ", spatialDomain="
                + spatialDomain + ", lifecycle=" + lifecycle + ", updatePhases=" + updatePhases + ']';
    }

    /** Indexes declaration values while preserving order and rejecting duplicate identities. */
    private static <K, V> Map<K, V> index(List<V> values, Function<V, K> key, String name) {
        Objects.requireNonNull(values, name);
        Map<K, V> result = new LinkedHashMap<>();
        for (V value : values) {
            V validValue = Objects.requireNonNull(value, name + " entry");
            K id = Objects.requireNonNull(key.apply(validValue), name + " id");
            if (result.putIfAbsent(id, validValue) != null) {
                throw new IllegalArgumentException(name + " contains a duplicate identity: " + id);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Copies one set while preserving declaration order and rejecting null entries. */
    private static <T> Set<T> immutableSet(Set<T> values, String name) {
        Objects.requireNonNull(values, name);
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (T value : values) {
            result.add(Objects.requireNonNull(value, name + " entry"));
        }
        return Collections.unmodifiableSet(result);
    }

    /** Mutable construction aid for the deliberately rich component descriptor. */
    public static final class Builder {
        private final ComponentType type;
        private final DescriptorPresentation presentation;
        private List<PropertyDescriptor> properties = List.of();
        private List<EndpointDescriptor> signals = List.of();
        private List<EndpointDescriptor> actions = List.of();
        private Set<CapabilityId> providedCapabilities = Set.of();
        private Set<CapabilityId> requiredCapabilities = Set.of();
        private Set<AttachmentPointId> attachments = Set.of();
        private ComponentMultiplicity multiplicity = ComponentMultiplicity.SINGLE;
        private Set<ComponentTypeId> conflicts = Set.of();
        private ComponentSpatialDomain spatialDomain = ComponentSpatialDomain.NONE;
        private Set<ComponentLifecycle> lifecycle = Set.of();
        private Set<ComponentUpdatePhase> updatePhases = Set.of();

        /** Stores required builder values. */
        private Builder(ComponentType type, DescriptorPresentation presentation) {
            this.type = Objects.requireNonNull(type, "type");
            this.presentation = Objects.requireNonNull(presentation, "presentation");
        }

        /**
         * Sets property declarations.
         *
         * @param values property declarations in display order
         * @return this builder
         */
        public Builder properties(List<PropertyDescriptor> values) {
            properties = List.copyOf(values);
            return this;
        }

        /**
         * Sets signal declarations.
         *
         * @param values signal declarations in display order
         * @return this builder
         */
        public Builder signals(List<EndpointDescriptor> values) {
            signals = List.copyOf(values);
            return this;
        }

        /**
         * Sets action declarations.
         *
         * @param values action declarations in display order
         * @return this builder
         */
        public Builder actions(List<EndpointDescriptor> values) {
            actions = List.copyOf(values);
            return this;
        }

        /**
         * Sets provided capabilities.
         *
         * @param values capabilities supplied to sibling components
         * @return this builder
         */
        public Builder providedCapabilities(Set<CapabilityId> values) {
            providedCapabilities = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Sets required capabilities.
         *
         * @param values capabilities requiring exactly one sibling provider
         * @return this builder
         */
        public Builder requiredCapabilities(Set<CapabilityId> values) {
            requiredCapabilities = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Sets named spatial attachments.
         *
         * @param values attachments supplied by this component
         * @return this builder
         */
        public Builder attachments(Set<AttachmentPointId> values) {
            attachments = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Sets per-entity multiplicity.
         *
         * @param value allowed multiplicity
         * @return this builder
         */
        public Builder multiplicity(ComponentMultiplicity value) {
            multiplicity = Objects.requireNonNull(value, "value");
            return this;
        }

        /**
         * Sets component types that may not coexist with this component.
         *
         * @param values conflicting component types
         * @return this builder
         */
        public Builder conflicts(Set<ComponentTypeId> values) {
            conflicts = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Sets the primary spatial authority supplied by this component.
         *
         * @param value spatial domain
         * @return this builder
         */
        public Builder spatialDomain(ComponentSpatialDomain value) {
            spatialDomain = Objects.requireNonNull(value, "value");
            return this;
        }

        /**
         * Sets semantic lifecycle participation.
         *
         * @param values lifecycle callbacks implemented by the runtime component
         * @return this builder
         */
        public Builder lifecycle(Set<ComponentLifecycle> values) {
            lifecycle = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Sets deterministic update participation.
         *
         * @param values update phases implemented by the runtime component
         * @return this builder
         */
        public Builder updatePhases(Set<ComponentUpdatePhase> values) {
            updatePhases = new LinkedHashSet<>(values);
            return this;
        }

        /**
         * Builds an immutable validated descriptor.
         *
         * @return component type descriptor
         */
        public ComponentTypeDescriptor build() {
            return new ComponentTypeDescriptor(this);
        }
    }
}
