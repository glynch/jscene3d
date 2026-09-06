/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.contract;

import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Immutable deliberately exported interface of one reusable entity definition. */
public final class EntityContract {
    private static final EntityContract EMPTY =
            new EntityContract(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private final Map<PropertyId, Parameter> parameters;
    private final Map<EndpointId, Signal> signals;
    private final Map<EndpointId, Action> actions;
    private final List<CapabilityId> capabilities;
    private final Map<AttachmentPointId, Attachment> attachments;
    private final Map<PropertyId, ResourceBinding> resourceBindings;

    /**
     * Creates an exported definition contract in declaration order.
     *
     * @param parameters configurable values
     * @param signals emitted endpoints
     * @param actions accepted endpoints
     * @param capabilities declared definition capabilities
     * @param attachments exported spatial targets
     * @param resourceBindings configurable resource targets
     */
    public EntityContract(
            List<Parameter> parameters,
            List<Signal> signals,
            List<Action> actions,
            List<CapabilityId> capabilities,
            List<Attachment> attachments,
            List<ResourceBinding> resourceBindings) {
        this.parameters = uniqueIndex(parameters, Parameter::id, "parameters");
        this.signals = uniqueIndex(signals, Signal::id, "signals");
        this.actions = uniqueIndex(actions, Action::id, "actions");
        this.capabilities = uniqueValues(capabilities, "capabilities");
        this.attachments = uniqueIndex(attachments, Attachment::id, "attachments");
        this.resourceBindings = uniqueIndex(resourceBindings, ResourceBinding::id, "resourceBindings");
        for (PropertyId id : this.parameters.keySet()) {
            if (this.resourceBindings.containsKey(id)) {
                throw new IllegalArgumentException("contract argument id is duplicated: " + id);
            }
        }
    }

    /**
     * Returns the shared empty contract.
     *
     * @return contract with no exported members
     */
    public static EntityContract empty() {
        return EMPTY;
    }

    /** Whether a public argument must be supplied by every placement. */
    public enum Requirement {
        /** Every placement must supply the argument. */
        REQUIRED,
        /** A placement may omit the argument. */
        OPTIONAL
    }

    /**
     * Returns exported parameters in declaration order.
     *
     * @return immutable parameter index
     */
    public Map<PropertyId, Parameter> parameters() {
        return parameters;
    }

    /**
     * Returns exported signals in declaration order.
     *
     * @return immutable signal index
     */
    public Map<EndpointId, Signal> signals() {
        return signals;
    }

    /**
     * Returns exported actions in declaration order.
     *
     * @return immutable action index
     */
    public Map<EndpointId, Action> actions() {
        return actions;
    }

    /**
     * Returns declared capabilities in declaration order.
     *
     * @return immutable capabilities
     */
    public List<CapabilityId> capabilities() {
        return capabilities;
    }

    /**
     * Returns exported spatial attachment points in declaration order.
     *
     * @return immutable attachment index
     */
    public Map<AttachmentPointId, Attachment> attachments() {
        return attachments;
    }

    /**
     * Returns exported resource bindings in declaration order.
     *
     * @return immutable resource-binding index
     */
    public Map<PropertyId, ResourceBinding> resourceBindings() {
        return resourceBindings;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof EntityContract contract
                && parameters.equals(contract.parameters)
                && signals.equals(contract.signals)
                && actions.equals(contract.actions)
                && capabilities.equals(contract.capabilities)
                && attachments.equals(contract.attachments)
                && resourceBindings.equals(contract.resourceBindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters, signals, actions, capabilities, attachments, resourceBindings);
    }

    @Override
    public String toString() {
        return "EntityContract[parameters=" + parameters + ", signals=" + signals + ", actions=" + actions
                + ", capabilities=" + capabilities + ", attachments=" + attachments + ", resourceBindings="
                + resourceBindings + ']';
    }

    /** Exported configurable value backed by a private property target.
     *
     * @param id stable public argument identity
     * @param valueKind accepted structural value kind
     * @param requirement whether every placement must supply the argument
     * @param target private property or nested placement argument
     */
    public record Parameter(PropertyId id, ProjectValueKind valueKind, Requirement requirement, PropertyTarget target) {
        /** Validates the parameter declaration. */
        public Parameter {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(requirement, "requirement");
            Objects.requireNonNull(target, "target");
        }

        /**
         * Returns whether every placement must supply the argument.
         *
         * @return {@code true} for a required argument
         */
        public boolean isRequired() {
            return requirement == Requirement.REQUIRED;
        }

        /**
         * Returns whether a value has the declared structural kind.
         *
         * @param value candidate argument
         * @return {@code true} when accepted
         */
        public boolean accepts(ProjectValue value) {
            return ProjectValueKind.of(value) == valueKind;
        }
    }

    /** Exported signal backed by a private or nested signal target. */
    public static final class Signal {
        private final EndpointId id;
        private final @Nullable RegisteredType payload;
        private final EndpointTarget target;

        /**
         * Creates a signal with no payload.
         *
         * @param id stable public signal identity
         * @param target private component or nested placement signal
         */
        public Signal(EndpointId id, EndpointTarget target) {
            this.id = Objects.requireNonNull(id, "id");
            payload = null;
            this.target = Objects.requireNonNull(target, "target");
        }

        /**
         * Creates a signal with a registered payload type.
         *
         * @param id stable public signal identity
         * @param payload registered payload type
         * @param target private component or nested placement signal
         */
        public Signal(EndpointId id, RegisteredType payload, EndpointTarget target) {
            this.id = Objects.requireNonNull(id, "id");
            this.payload = Objects.requireNonNull(payload, "payload");
            this.target = Objects.requireNonNull(target, "target");
        }

        /**
         * Returns the stable public signal identity.
         *
         * @return signal identity
         */
        public EndpointId id() {
            return id;
        }

        /**
         * Returns the registered payload type when the signal carries a value.
         *
         * @return optional payload type
         */
        public Optional<RegisteredType> payload() {
            return Optional.ofNullable(payload);
        }

        /**
         * Returns the private component or nested placement signal target.
         *
         * @return signal target
         */
        public EndpointTarget target() {
            return target;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof Signal signal
                    && id.equals(signal.id)
                    && Objects.equals(payload, signal.payload)
                    && target.equals(signal.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, payload, target);
        }

        @Override
        public String toString() {
            return "Signal[id=" + id + ", payload=" + payload + ", target=" + target + ']';
        }
    }

    /** Exported action backed by a private or nested action target. */
    public static final class Action {
        private final EndpointId id;
        private final @Nullable RegisteredType payload;
        private final EndpointTarget target;

        /**
         * Creates an action with no payload.
         *
         * @param id stable public action identity
         * @param target private component or nested placement action
         */
        public Action(EndpointId id, EndpointTarget target) {
            this.id = Objects.requireNonNull(id, "id");
            payload = null;
            this.target = Objects.requireNonNull(target, "target");
        }

        /**
         * Creates an action with a registered payload type.
         *
         * @param id stable public action identity
         * @param payload registered payload type
         * @param target private component or nested placement action
         */
        public Action(EndpointId id, RegisteredType payload, EndpointTarget target) {
            this.id = Objects.requireNonNull(id, "id");
            this.payload = Objects.requireNonNull(payload, "payload");
            this.target = Objects.requireNonNull(target, "target");
        }

        /**
         * Returns the stable public action identity.
         *
         * @return action identity
         */
        public EndpointId id() {
            return id;
        }

        /**
         * Returns the registered payload type when the action accepts a value.
         *
         * @return optional payload type
         */
        public Optional<RegisteredType> payload() {
            return Optional.ofNullable(payload);
        }

        /**
         * Returns the private component or nested placement action target.
         *
         * @return action target
         */
        public EndpointTarget target() {
            return target;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof Action action
                    && id.equals(action.id)
                    && Objects.equals(payload, action.payload)
                    && target.equals(action.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, payload, target);
        }

        @Override
        public String toString() {
            return "Action[id=" + id + ", payload=" + payload + ", target=" + target + ']';
        }
    }

    /** Exported named spatial target.
     *
     * @param id stable public attachment identity
     * @param target private or nested spatial target
     */
    public record Attachment(AttachmentPointId id, SpatialTarget target) {
        /** Validates the attachment declaration. */
        public Attachment {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
        }
    }

    /** Exported resource argument backed by a private property or nested binding.
     *
     * @param id stable public argument identity
     * @param requirement whether every placement must supply the binding
     * @param acceptedKinds accepted reference namespaces, or empty for any namespace
     * @param target private property or nested placement binding
     */
    public record ResourceBinding(
            PropertyId id, Requirement requirement, Set<ResourceReference.Kind> acceptedKinds, PropertyTarget target) {
        /** Validates and copies the binding declaration. */
        public ResourceBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(requirement, "requirement");
            acceptedKinds = Set.copyOf(acceptedKinds);
            Objects.requireNonNull(target, "target");
        }

        /**
         * Returns whether every placement must supply the binding.
         *
         * @return {@code true} for a required binding
         */
        public boolean isRequired() {
            return requirement == Requirement.REQUIRED;
        }

        /**
         * Returns whether a value is a resource reference in an accepted namespace.
         *
         * @param value candidate argument
         * @return {@code true} when accepted
         */
        public boolean accepts(ProjectValue value) {
            return value instanceof ProjectValue.ReferenceValue reference
                    && (acceptedKinds.isEmpty()
                            || acceptedKinds.contains(reference.reference().kind()));
        }
    }

    /** Copies values into a declaration-ordered index and rejects duplicate identities. */
    private static <K, V> Map<K, V> uniqueIndex(List<V> values, Function<V, K> key, String name) {
        Objects.requireNonNull(values, name);
        Map<K, V> index = new LinkedHashMap<>();
        for (V value : values) {
            V validValue = Objects.requireNonNull(value, name + " entry");
            K id = Objects.requireNonNull(key.apply(validValue), name + " id");
            if (index.putIfAbsent(id, validValue) != null) {
                throw new IllegalArgumentException(name + " contains a duplicate id: " + id);
            }
        }
        return Collections.unmodifiableMap(index);
    }

    /** Copies values in declaration order and rejects duplicates. */
    private static <T> List<T> uniqueValues(List<T> values, String name) {
        List<T> copied = List.copyOf(values);
        Set<T> unique = new HashSet<>(copied);
        if (unique.size() != copied.size()) {
            throw new IllegalArgumentException(name + " contains a duplicate value");
        }
        return copied;
    }
}
