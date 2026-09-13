/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Descriptor for one project-authored property exposed by a registered type. */
public final class PropertyDescriptor {
    private final String id;
    private final ProjectValueKind valueKind;
    private final Optional<ProjectValueKind> elementKind;
    private final Optional<Integer> exactElementCount;
    private final boolean required;
    private final Optional<ProjectValue> defaultValue;
    private final DescriptorPresentation presentation;
    private final Map<String, ProjectValue> editorMetadata;
    private final Set<ResourceReference.Kind> acceptedReferenceKinds;

    /** Stores one validated property descriptor. */
    private PropertyDescriptor(
            String id,
            ValueShape valueShape,
            boolean required,
            Optional<ProjectValue> defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata,
            Set<ResourceReference.Kind> acceptedReferenceKinds) {
        this.id = requireLocalId(id, "id");
        ValueShape validShape = Objects.requireNonNull(valueShape, "valueShape");
        valueKind = validShape.valueKind();
        elementKind = validShape.elementKind();
        exactElementCount = validShape.exactElementCount();
        this.required = required;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.editorMetadata = immutableProjectValues(editorMetadata, "editorMetadata");
        this.acceptedReferenceKinds =
                Set.copyOf(Objects.requireNonNull(acceptedReferenceKinds, "acceptedReferenceKinds"));
        if (valueKind != ProjectValueKind.REFERENCE && !this.acceptedReferenceKinds.isEmpty()) {
            throw new IllegalArgumentException("acceptedReferenceKinds require a reference property");
        }
        if (this.elementKind.isPresent() && valueKind != ProjectValueKind.ARRAY) {
            throw new IllegalArgumentException("elementKind requires an array property");
        }
        if ((isAuthoredTarget(valueKind)
                        || this.elementKind
                                .filter(PropertyDescriptor::isAuthoredTarget)
                                .isPresent())
                && this.defaultValue.isPresent()) {
            throw new IllegalArgumentException("authored target properties cannot declare defaults");
        }
        this.defaultValue.ifPresent(this::requireAcceptedValue);
    }

    /**
     * Creates a required property without a default.
     *
     * @param id stable local property identifier
     * @param valueKind structural value kind
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @param acceptedReferenceKinds accepted reference namespaces
     * @return required property descriptor
     */
    public static PropertyDescriptor required(
            String id,
            ProjectValueKind valueKind,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata,
            Set<ResourceReference.Kind> acceptedReferenceKinds) {
        return new PropertyDescriptor(
                id,
                ValueShape.scalar(valueKind),
                true,
                Optional.empty(),
                presentation,
                editorMetadata,
                acceptedReferenceKinds);
    }

    /**
     * Creates a required array whose elements all have one structural kind.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return required homogeneous array property descriptor
     */
    public static PropertyDescriptor requiredArray(
            String id,
            ProjectValueKind elementKind,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id, ValueShape.array(elementKind), true, Optional.empty(), presentation, editorMetadata, Set.of());
    }

    /**
     * Creates a required homogeneous array with an exact element count.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param exactElementCount required number of array elements
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return required fixed-size homogeneous array property descriptor
     */
    public static PropertyDescriptor requiredArray(
            String id,
            ProjectValueKind elementKind,
            int exactElementCount,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id,
                ValueShape.array(elementKind, exactElementCount),
                true,
                Optional.empty(),
                presentation,
                editorMetadata,
                Set.of());
    }

    /**
     * Creates an optional array whose elements all have one structural kind.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return optional homogeneous array property descriptor
     */
    public static PropertyDescriptor optionalArray(
            String id,
            ProjectValueKind elementKind,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id, ValueShape.array(elementKind), false, Optional.empty(), presentation, editorMetadata, Set.of());
    }

    /**
     * Creates an optional homogeneous array with an exact element count.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param exactElementCount required number of array elements
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return optional fixed-size homogeneous array property descriptor
     */
    public static PropertyDescriptor optionalArray(
            String id,
            ProjectValueKind elementKind,
            int exactElementCount,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id,
                ValueShape.array(elementKind, exactElementCount),
                false,
                Optional.empty(),
                presentation,
                editorMetadata,
                Set.of());
    }

    /**
     * Creates an optional array with a homogeneous default value.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param defaultValue default array value
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return optional homogeneous array property descriptor with a default
     */
    public static PropertyDescriptor optionalArrayWithDefault(
            String id,
            ProjectValueKind elementKind,
            ProjectValue.ArrayValue defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id,
                ValueShape.array(elementKind),
                false,
                Optional.of(defaultValue),
                presentation,
                editorMetadata,
                Set.of());
    }

    /**
     * Creates an optional fixed-size homogeneous array with a default value.
     *
     * @param id stable local property identifier
     * @param elementKind required structural kind for every array element
     * @param exactElementCount required number of array elements
     * @param defaultValue default array value
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @return optional fixed-size homogeneous array property descriptor with a default
     */
    public static PropertyDescriptor optionalArrayWithDefault(
            String id,
            ProjectValueKind elementKind,
            int exactElementCount,
            ProjectValue.ArrayValue defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata) {
        return new PropertyDescriptor(
                id,
                ValueShape.array(elementKind, exactElementCount),
                false,
                Optional.of(defaultValue),
                presentation,
                editorMetadata,
                Set.of());
    }

    /**
     * Creates an optional property without a default.
     *
     * @param id stable local property identifier
     * @param valueKind structural value kind
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @param acceptedReferenceKinds accepted reference namespaces
     * @return optional property descriptor
     */
    public static PropertyDescriptor optional(
            String id,
            ProjectValueKind valueKind,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata,
            Set<ResourceReference.Kind> acceptedReferenceKinds) {
        return new PropertyDescriptor(
                id,
                ValueShape.scalar(valueKind),
                false,
                Optional.empty(),
                presentation,
                editorMetadata,
                acceptedReferenceKinds);
    }

    /**
     * Creates an optional property with a default.
     *
     * @param id stable local property identifier
     * @param valueKind structural value kind
     * @param defaultValue default project value
     * @param presentation human-readable property metadata
     * @param editorMetadata generic editor hints
     * @param acceptedReferenceKinds accepted reference namespaces
     * @return optional property descriptor with a default
     */
    public static PropertyDescriptor optionalWithDefault(
            String id,
            ProjectValueKind valueKind,
            ProjectValue defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata,
            Set<ResourceReference.Kind> acceptedReferenceKinds) {
        return new PropertyDescriptor(
                id,
                ValueShape.scalar(valueKind),
                false,
                Optional.of(defaultValue),
                presentation,
                editorMetadata,
                acceptedReferenceKinds);
    }

    /**
     * Returns the stable local property identifier.
     *
     * @return property identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the required structural value kind.
     *
     * @return value kind
     */
    public ProjectValueKind valueKind() {
        return valueKind;
    }

    /**
     * Returns the required homogeneous array-element kind, when declared.
     *
     * @return optional element kind
     */
    public Optional<ProjectValueKind> elementKind() {
        return elementKind;
    }

    /**
     * Returns the exact required array element count, when declared.
     *
     * @return optional exact element count
     */
    public Optional<Integer> exactElementCount() {
        return exactElementCount;
    }

    /**
     * Returns whether authored data must provide this property.
     *
     * @return {@code true} when authored data must provide the property
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the optional default applied when authored data omits the property.
     *
     * @return optional default value
     */
    public Optional<ProjectValue> defaultValue() {
        return defaultValue;
    }

    /**
     * Returns human-readable property metadata.
     *
     * @return presentation metadata
     */
    public DescriptorPresentation presentation() {
        return presentation;
    }

    /**
     * Returns immutable editor-specific metadata in declaration order.
     *
     * @return editor metadata
     */
    public Map<String, ProjectValue> editorMetadata() {
        return editorMetadata;
    }

    /**
     * Returns accepted resource-reference namespaces, or an empty set for any namespace.
     *
     * @return accepted reference namespaces
     */
    public Set<ResourceReference.Kind> acceptedReferenceKinds() {
        return acceptedReferenceKinds;
    }

    /**
     * Returns whether a project value satisfies this descriptor's structural constraints.
     *
     * @param value project value to check
     * @return {@code true} when the value is accepted
     */
    public boolean accepts(ProjectValue value) {
        ProjectValue validValue = Objects.requireNonNull(value, "value");
        if (ProjectValueKind.of(validValue) != valueKind) {
            return false;
        }
        if (validValue instanceof ProjectValue.ReferenceValue referenceValue) {
            return acceptedReferenceKinds.isEmpty()
                    || acceptedReferenceKinds.contains(
                            referenceValue.reference().kind());
        }
        if (!(validValue instanceof ProjectValue.ArrayValue array)) {
            return true;
        }
        if (exactElementCount.isPresent() && array.values().size() != exactElementCount.orElseThrow()) {
            return false;
        }
        return elementKind.isEmpty()
                || array.values().stream()
                        .allMatch(element -> ProjectValueKind.of(element) == elementKind.orElseThrow());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PropertyDescriptor descriptor
                && required == descriptor.required
                && id.equals(descriptor.id)
                && valueKind == descriptor.valueKind
                && elementKind.equals(descriptor.elementKind)
                && exactElementCount.equals(descriptor.exactElementCount)
                && defaultValue.equals(descriptor.defaultValue)
                && presentation.equals(descriptor.presentation)
                && editorMetadata.equals(descriptor.editorMetadata)
                && acceptedReferenceKinds.equals(descriptor.acceptedReferenceKinds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                valueKind,
                elementKind,
                exactElementCount,
                required,
                defaultValue,
                presentation,
                editorMetadata,
                acceptedReferenceKinds);
    }

    @Override
    public String toString() {
        return "PropertyDescriptor[id=" + id + ", valueKind=" + valueKind + ", elementKind=" + elementKind
                + ", exactElementCount=" + exactElementCount + ", required=" + required + ", defaultValue="
                + defaultValue + ", presentation=" + presentation + ", editorMetadata=" + editorMetadata
                + ", acceptedReferenceKinds=" + acceptedReferenceKinds + ']';
    }

    /** Rejects a default inconsistent with this property. */
    private void requireAcceptedValue(ProjectValue value) {
        if (!accepts(value)) {
            throw new IllegalArgumentException("defaultValue does not satisfy property " + id);
        }
    }

    /** Returns whether one kind requires an authored instance scope unavailable to descriptor defaults. */
    private static boolean isAuthoredTarget(ProjectValueKind kind) {
        return kind == ProjectValueKind.ENTITY_TARGET || kind == ProjectValueKind.COMPONENT_TARGET;
    }

    /** Structural value declaration kept cohesive so constructor arity remains bounded. */
    private record ValueShape(
            ProjectValueKind valueKind, Optional<ProjectValueKind> elementKind, Optional<Integer> exactElementCount) {
        /** Validates array-only shape constraints. */
        private ValueShape {
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(elementKind, "elementKind");
            Objects.requireNonNull(exactElementCount, "exactElementCount");
            if (exactElementCount.filter(count -> count <= 0).isPresent()) {
                throw new IllegalArgumentException("exactElementCount must be positive");
            }
            if (exactElementCount.isPresent() && valueKind != ProjectValueKind.ARRAY) {
                throw new IllegalArgumentException("exactElementCount requires an array property");
            }
        }

        /** Creates one scalar or unconstrained-container declaration. */
        private static ValueShape scalar(ProjectValueKind valueKind) {
            return new ValueShape(Objects.requireNonNull(valueKind, "valueKind"), Optional.empty(), Optional.empty());
        }

        /** Creates one homogeneous array declaration. */
        private static ValueShape array(ProjectValueKind elementKind) {
            return new ValueShape(
                    ProjectValueKind.ARRAY,
                    Optional.of(Objects.requireNonNull(elementKind, "elementKind")),
                    Optional.empty());
        }

        /** Creates one exact-size homogeneous array declaration. */
        private static ValueShape array(ProjectValueKind elementKind, int exactElementCount) {
            return new ValueShape(
                    ProjectValueKind.ARRAY,
                    Optional.of(Objects.requireNonNull(elementKind, "elementKind")),
                    Optional.of(exactElementCount));
        }
    }
}
