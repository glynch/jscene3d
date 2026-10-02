/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionSnapshot.Occurrence;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Explicit complete wire snapshot for one selected semantic Inspector target.
 *
 * @param revision authoritative definition/session revision
 * @param target validated Java-issued semantic target
 * @param title locale-resolved selected-object title
 * @param definitionOrigin authored or generated retained-definition origin
 * @param provenance local or generated occurrence provenance
 * @param editable whether the selected target has an authoritative mutation path
 * @param groups ordered selectable target groups
 */
public record InspectorSnapshot(
        long revision,
        DefinitionSnapshot.SemanticTarget target,
        String title,
        String definitionOrigin,
        String provenance,
        boolean editable,
        List<TargetGroup> groups) {
    /** Copies and validates snapshot data. */
    public InspectorSnapshot {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(definitionOrigin, "definitionOrigin");
        Objects.requireNonNull(provenance, "provenance");
        groups = List.copyOf(groups);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    /** Exact registered component type.
     *
     * @param id registered component type identity
     * @param version positive type version
     */
    public record ComponentTypeDto(String id, int version) {
        /** Validates component type identity. */
        public ComponentTypeDto {
            Objects.requireNonNull(id, "id");
            if (version < 1) {
                throw new IllegalArgumentException("component type version must be positive");
            }
        }
    }

    /** One selectable group in authoritative order.
     *
     * @param identity stable identity within the snapshot
     * @param kind entity, component, or placement
     * @param label locale-resolved display label
     * @param description optional locale-resolved description
     * @param componentId component instance identity when kind is component
     * @param componentType exact component type when kind is component
     * @param metadataStatus available or unavailable
     * @param editable whether this group has valid future mutation identities
     * @param properties ordered properties
     */
    public record TargetGroup(
            String identity,
            String kind,
            String label,
            @Nullable String description,
            @Nullable String componentId,
            @Nullable ComponentTypeDto componentType,
            String metadataStatus,
            boolean editable,
            List<Property> properties) {
        /** Copies and validates group data. */
        public TargetGroup {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(metadataStatus, "metadataStatus");
            properties = List.copyOf(properties);
            if ((componentId == null) != (componentType == null)) {
                throw new IllegalArgumentException("component identity and type must be present together");
            }
            if ("component".equals(kind) != (componentId != null)) {
                throw new IllegalArgumentException("only component groups carry component identity");
            }
        }
    }

    /** One property in descriptor or authored fallback order.
     *
     * @param identity stable property identity
     * @param label locale-resolved display label
     * @param description optional locale-resolved description
     * @param valueKind structural project-value kind
     * @param required whether the descriptor requires an authored value
     * @param constraints structural and typed semantic constraints
     * @param state authored/default/effective and validity state
     * @param mutationTarget optional valid future mutation identity
     */
    public record Property(
            String identity,
            String label,
            @Nullable String description,
            String valueKind,
            boolean required,
            Constraints constraints,
            PropertyState state,
            @Nullable MutationTarget mutationTarget) {
        /** Validates property data. */
        public Property {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(constraints, "constraints");
            Objects.requireNonNull(state, "state");
            if ((mutationTarget != null) != state.editable()) {
                throw new IllegalArgumentException("editable Inspector properties require a mutation target");
            }
        }
    }

    /** Structural and descriptor-owned semantic constraints.
     *
     * @param elementKind homogeneous array element kind
     * @param exactElementCount fixed array size
     * @param acceptedReferenceKinds accepted reference namespaces
     * @param editor typed semantic editor description
     */
    public record Constraints(
            @Nullable String elementKind,
            @Nullable Integer exactElementCount,
            List<String> acceptedReferenceKinds,
            EditorSemantics editor) {
        /** Copies and validates constraints. */
        public Constraints {
            acceptedReferenceKinds = List.copyOf(acceptedReferenceKinds);
            Objects.requireNonNull(editor, "editor");
            if (exactElementCount != null && exactElementCount < 1) {
                throw new IllegalArgumentException("exactElementCount must be positive");
            }
        }
    }

    /** Closed descriptor-owned semantic editor vocabulary.
     *
     * @param semantic stable semantic name
     * @param minimum optional exact lower bound
     * @param maximum optional exact upper bound
     */
    public record EditorSemantics(
            String semantic,
            @Nullable NumericBound minimum,
            @Nullable NumericBound maximum) {
        /** Validates editor semantics. */
        public EditorSemantics {
            Objects.requireNonNull(semantic, "semantic");
        }
    }

    /** Exact inclusive or exclusive numeric endpoint.
     *
     * @param decimal canonical decimal text
     * @param inclusive whether the endpoint itself is accepted
     */
    public record NumericBound(String decimal, boolean inclusive) {
        /** Validates exact decimal text. */
        public NumericBound {
            Objects.requireNonNull(decimal, "decimal");
        }
    }

    /** Value state independent of structural constraints.
     *
     * @param authoredValue explicitly authored value
     * @param defaultValue descriptor default
     * @param effectiveValue authored or default value
     * @param origin authored, default, or unset
     * @param validity current semantic validity
     * @param editable whether a mutation target is present
     * @param modified whether the authored target differs from its persisted baseline
     */
    public record PropertyState(
            @Nullable Value authoredValue,
            @Nullable Value defaultValue,
            @Nullable Value effectiveValue,
            String origin,
            String validity,
            boolean editable,
            boolean modified) {
        /** Validates state labels. */
        public PropertyState {
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(validity, "validity");
        }
    }

    /** Explicit tagged Inspector value hierarchy. */
    public sealed interface Value
            permits NullValue,
                    BooleanValue,
                    NumberValue,
                    TextValue,
                    ArrayValue,
                    ObjectValue,
                    ReferenceValue,
                    EntityTargetValue,
                    ComponentTargetValue {}

    /** Explicit null value.
     *
     * @param kind fixed {@code null} tag
     */
    public record NullValue(String kind) implements Value {
        /** Validates the fixed tag. */
        public NullValue {
            requireKind(kind, "null");
        }
    }

    /** Boolean value.
     *
     * @param kind fixed {@code boolean} tag
     * @param value stored value
     */
    public record BooleanValue(String kind, boolean value) implements Value {
        /** Validates the fixed tag. */
        public BooleanValue {
            requireKind(kind, "boolean");
        }
    }

    /** Exact decimal value.
     *
     * @param kind fixed {@code number} tag
     * @param decimal canonical decimal text
     */
    public record NumberValue(String kind, String decimal) implements Value {
        /** Validates the fixed tag and exact text. */
        public NumberValue {
            requireKind(kind, "number");
            Objects.requireNonNull(decimal, "decimal");
        }
    }

    /** Text value.
     *
     * @param kind fixed {@code text} tag
     * @param value stored text
     */
    public record TextValue(String kind, String value) implements Value {
        /** Validates the fixed tag. */
        public TextValue {
            requireKind(kind, "text");
            Objects.requireNonNull(value, "value");
        }
    }

    /** Ordered array value.
     *
     * @param kind fixed {@code array} tag
     * @param values ordered values
     */
    public record ArrayValue(String kind, List<Value> values) implements Value {
        /** Copies values and validates the fixed tag. */
        public ArrayValue {
            requireKind(kind, "array");
            values = List.copyOf(values);
        }
    }

    /** Ordered object value without an implied member schema.
     *
     * @param kind fixed {@code object} tag
     * @param values ordered named values
     */
    public record ObjectValue(String kind, Map<String, Value> values) implements Value {
        /** Copies values in declaration order and validates the fixed tag. */
        public ObjectValue {
            requireKind(kind, "object");
            LinkedHashMap<String, Value> copy = new LinkedHashMap<>();
            values.forEach((key, value) ->
                    copy.put(Objects.requireNonNull(key, "object key"), Objects.requireNonNull(value, "object value")));
            values = Collections.unmodifiableMap(copy);
        }
    }

    /** Semantic resource reference.
     *
     * @param kind fixed {@code reference} tag
     * @param referenceKind portable namespace
     * @param locator portable locator
     * @param label resolved display label or locator fallback
     * @param resolution resolved or broken
     * @param revealUri optional source URI
     */
    public record ReferenceValue(
            String kind,
            String referenceKind,
            String locator,
            String label,
            String resolution,
            @Nullable String revealUri)
            implements Value {
        /** Validates reference presentation. */
        public ReferenceValue {
            requireKind(kind, "reference");
            Objects.requireNonNull(referenceKind, "referenceKind");
            Objects.requireNonNull(locator, "locator");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(resolution, "resolution");
        }
    }

    /** Semantic entity target.
     *
     * @param kind fixed {@code entity-target} tag
     * @param entityId stable entity identity
     * @param label resolved authored label or identity fallback
     * @param resolution resolved or broken
     * @param occurrence optional hierarchy reveal identity
     */
    public record EntityTargetValue(
            String kind,
            String entityId,
            String label,
            String resolution,
            @Nullable Occurrence occurrence) implements Value {
        /** Validates entity-target presentation. */
        public EntityTargetValue {
            requireKind(kind, "entity-target");
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(resolution, "resolution");
        }
    }

    /** Semantic component target.
     *
     * @param kind fixed {@code component-target} tag
     * @param entityId owning entity identity
     * @param componentId component identity
     * @param entityLabel resolved entity label or identity fallback
     * @param componentLabel resolved descriptor label or identity fallback
     * @param componentType exact component type when resolved
     * @param resolution resolved or broken
     * @param occurrence optional owning-entity reveal identity
     */
    public record ComponentTargetValue(
            String kind,
            String entityId,
            String componentId,
            String entityLabel,
            String componentLabel,
            @Nullable ComponentTypeDto componentType,
            String resolution,
            @Nullable Occurrence occurrence)
            implements Value {
        /** Validates component-target presentation. */
        public ComponentTargetValue {
            requireKind(kind, "component-target");
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(componentId, "componentId");
            Objects.requireNonNull(entityLabel, "entityLabel");
            Objects.requireNonNull(componentLabel, "componentLabel");
            Objects.requireNonNull(resolution, "resolution");
        }
    }

    /** Explicit future mutation identity hierarchy. */
    public sealed interface MutationTarget permits EntityEnabledMutation, ComponentPropertyMutation {}

    /** Entity-enabled mutation identity.
     *
     * @param kind fixed {@code entity-enabled} tag
     * @param occurrence editable occurrence
     * @param entityId entity or placement identity
     */
    public record EntityEnabledMutation(String kind, DefinitionSnapshot.Occurrence occurrence, String entityId)
            implements MutationTarget {
        /** Validates mutation identity. */
        public EntityEnabledMutation {
            requireKind(kind, "entity-enabled");
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(entityId, "entityId");
        }
    }

    /** Component-property mutation identity.
     *
     * @param kind fixed {@code component-property} tag
     * @param occurrence editable occurrence
     * @param entityId entity identity
     * @param componentId component identity
     * @param propertyId property identity
     */
    public record ComponentPropertyMutation(
            String kind,
            DefinitionSnapshot.Occurrence occurrence,
            String entityId,
            String componentId,
            String propertyId)
            implements MutationTarget {
        /** Validates mutation identity. */
        public ComponentPropertyMutation {
            requireKind(kind, "component-property");
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(componentId, "componentId");
            Objects.requireNonNull(propertyId, "propertyId");
        }
    }

    /** Requires one fixed discriminant without relying on Java class names. */
    private static void requireKind(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("kind must be " + expected);
        }
    }
}
