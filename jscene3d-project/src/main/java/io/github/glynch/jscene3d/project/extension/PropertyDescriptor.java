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
    private final boolean required;
    private final Optional<ProjectValue> defaultValue;
    private final DescriptorPresentation presentation;
    private final Map<String, ProjectValue> editorMetadata;
    private final Set<ResourceReference.Kind> acceptedReferenceKinds;

    /** Stores one validated property descriptor. */
    private PropertyDescriptor(
            String id,
            ProjectValueKind valueKind,
            boolean required,
            Optional<ProjectValue> defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editorMetadata,
            Set<ResourceReference.Kind> acceptedReferenceKinds) {
        this.id = requireLocalId(id, "id");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.required = required;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.editorMetadata = immutableProjectValues(editorMetadata, "editorMetadata");
        this.acceptedReferenceKinds =
                Set.copyOf(Objects.requireNonNull(acceptedReferenceKinds, "acceptedReferenceKinds"));
        if (valueKind != ProjectValueKind.REFERENCE && !this.acceptedReferenceKinds.isEmpty()) {
            throw new IllegalArgumentException("acceptedReferenceKinds require a reference property");
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
                id, valueKind, true, Optional.empty(), presentation, editorMetadata, acceptedReferenceKinds);
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
                id, valueKind, false, Optional.empty(), presentation, editorMetadata, acceptedReferenceKinds);
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
                id, valueKind, false, Optional.of(defaultValue), presentation, editorMetadata, acceptedReferenceKinds);
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
        return !(validValue instanceof ProjectValue.ReferenceValue referenceValue)
                || acceptedReferenceKinds.isEmpty()
                || acceptedReferenceKinds.contains(referenceValue.reference().kind());
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
                && defaultValue.equals(descriptor.defaultValue)
                && presentation.equals(descriptor.presentation)
                && editorMetadata.equals(descriptor.editorMetadata)
                && acceptedReferenceKinds.equals(descriptor.acceptedReferenceKinds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, valueKind, required, defaultValue, presentation, editorMetadata, acceptedReferenceKinds);
    }

    @Override
    public String toString() {
        return "PropertyDescriptor[id=" + id + ", valueKind=" + valueKind + ", required=" + required
                + ", defaultValue=" + defaultValue + ", presentation=" + presentation + ", editorMetadata="
                + editorMetadata + ", acceptedReferenceKinds=" + acceptedReferenceKinds + ']';
    }

    /** Rejects a default inconsistent with this property. */
    private void requireAcceptedValue(ProjectValue value) {
        if (!accepts(value)) {
            throw new IllegalArgumentException("defaultValue does not satisfy property " + id);
        }
    }
}
