/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Validated descriptor-owned editor semantics independent of any concrete UI control. */
public final class PropertyEditorDescriptor {
    private static final String MINIMUM_METADATA_KEY = "minimum";
    private static final String MINIMUM_EXCLUSIVE = "minimum-exclusive";
    private static final String MAXIMUM_METADATA_KEY = "maximum";
    private static final String MAXIMUM_EXCLUSIVE = "maximum-exclusive";

    private final PropertyEditorSemantic semantic;
    private final Optional<PropertyNumericBound> minimum;
    private final Optional<PropertyNumericBound> maximum;

    /** Stores one validated semantic descriptor. */
    private PropertyEditorDescriptor(
            PropertyEditorSemantic semantic,
            Optional<PropertyNumericBound> minimum,
            Optional<PropertyNumericBound> maximum) {
        this.semantic = Objects.requireNonNull(semantic, "semantic");
        this.minimum = Objects.requireNonNull(minimum, "minimum");
        this.maximum = Objects.requireNonNull(maximum, "maximum");
        if (minimum.isPresent()
                && maximum.isPresent()
                && minimum.orElseThrow().value().compareTo(maximum.orElseThrow().value()) > 0) {
            throw new IllegalArgumentException("minimum editor bound must not exceed maximum");
        }
    }

    /**
     * Returns the descriptor for a property without specialized semantics.
     *
     * @return default editor descriptor
     */
    public static PropertyEditorDescriptor defaultEditor() {
        return new PropertyEditorDescriptor(PropertyEditorSemantic.DEFAULT, Optional.empty(), Optional.empty());
    }

    /** Builds typed semantics from canonical metadata while leaving unknown keys unexposed. */
    static PropertyEditorDescriptor from(
            ProjectValueKind valueKind,
            Optional<ProjectValueKind> elementKind,
            Optional<Integer> exactElementCount,
            Map<String, ProjectValue> metadata) {
        Objects.requireNonNull(valueKind, "valueKind");
        Objects.requireNonNull(elementKind, "elementKind");
        Objects.requireNonNull(exactElementCount, "exactElementCount");
        Objects.requireNonNull(metadata, "metadata");
        PropertyEditorSemantic semantic = semantic(metadata);
        requireCompatibleShape(semantic, valueKind, elementKind, exactElementCount);
        Optional<PropertyNumericBound> minimum = bound(metadata, MINIMUM_METADATA_KEY, MINIMUM_EXCLUSIVE);
        Optional<PropertyNumericBound> maximum = bound(metadata, MAXIMUM_METADATA_KEY, MAXIMUM_EXCLUSIVE);
        if ((minimum.isPresent() || maximum.isPresent()) && valueKind != ProjectValueKind.NUMBER) {
            throw new IllegalArgumentException("numeric editor bounds require a number property");
        }
        return new PropertyEditorDescriptor(semantic, minimum, maximum);
    }

    /**
     * Returns the closed semantic editor meaning.
     *
     * @return semantic meaning
     */
    public PropertyEditorSemantic semantic() {
        return semantic;
    }

    /**
     * Returns the optional exact lower bound.
     *
     * @return lower bound
     */
    public Optional<PropertyNumericBound> minimum() {
        return minimum;
    }

    /**
     * Returns the optional exact upper bound.
     *
     * @return upper bound
     */
    public Optional<PropertyNumericBound> maximum() {
        return maximum;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PropertyEditorDescriptor descriptor
                && semantic == descriptor.semantic
                && minimum.equals(descriptor.minimum)
                && maximum.equals(descriptor.maximum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semantic, minimum, maximum);
    }

    @Override
    public String toString() {
        return "PropertyEditorDescriptor[semantic=" + semantic + ", minimum=" + minimum + ", maximum=" + maximum + ']';
    }

    /** Resolves a known string semantic, treating absent or unknown extension metadata as default. */
    private static PropertyEditorSemantic semantic(Map<String, ProjectValue> metadata) {
        ProjectValue value = metadata.get(PropertyDescriptorKeys.EDITOR_SEMANTIC);
        if (value == null) {
            return PropertyEditorSemantic.DEFAULT;
        }
        if (!(value instanceof ProjectValue.TextValue text)) {
            throw new IllegalArgumentException("editor semantic must be text");
        }
        return PropertyEditorSemantic.fromMetadata(text.value());
    }

    /** Resolves one mutually exclusive inclusive or exclusive bound. */
    private static Optional<PropertyNumericBound> bound(
            Map<String, ProjectValue> metadata, String inclusiveKey, String exclusiveKey) {
        ProjectValue inclusive = metadata.get(inclusiveKey);
        ProjectValue exclusive = metadata.get(exclusiveKey);
        if (inclusive != null && exclusive != null) {
            throw new IllegalArgumentException(
                    "editor metadata cannot declare both " + inclusiveKey + " and " + exclusiveKey);
        }
        if (inclusive != null) {
            return Optional.of(new PropertyNumericBound(number(inclusive, inclusiveKey), true));
        }
        if (exclusive != null) {
            return Optional.of(new PropertyNumericBound(number(exclusive, exclusiveKey), false));
        }
        return Optional.empty();
    }

    /** Returns one exact numeric metadata value. */
    private static BigDecimal number(ProjectValue value, String key) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(key + " editor metadata must be a number");
        }
        return number.value();
    }

    /** Rejects specialized semantics whose explicitly declared shape is incompatible. */
    private static void requireCompatibleShape(
            PropertyEditorSemantic semantic,
            ProjectValueKind valueKind,
            Optional<ProjectValueKind> elementKind,
            Optional<Integer> exactElementCount) {
        switch (semantic) {
            case DEFAULT -> {
                // Default semantics impose no additional shape requirements.
            }
            case INTEGER -> {
                if (valueKind != ProjectValueKind.NUMBER) {
                    throw new IllegalArgumentException("integer editor semantic requires a number property");
                }
            }
            case VECTOR2 -> requireFixedNumericArray(valueKind, elementKind, exactElementCount, 2, "vector2");
            case VECTOR3 -> requireFixedNumericArray(valueKind, elementKind, exactElementCount, 3, "vector3");
            case QUATERNION -> requireFixedNumericArray(valueKind, elementKind, exactElementCount, 4, "quaternion");
            case LINEAR_COLOR -> requireFixedNumericArray(valueKind, elementKind, exactElementCount, 3, "linear color");
        }
    }

    /** Requires one explicitly sized homogeneous numeric array. */
    private static void requireFixedNumericArray(
            ProjectValueKind valueKind,
            Optional<ProjectValueKind> elementKind,
            Optional<Integer> exactElementCount,
            int count,
            String semantic) {
        if (valueKind != ProjectValueKind.ARRAY
                || elementKind.filter(ProjectValueKind.NUMBER::equals).isEmpty()
                || exactElementCount.filter(value -> value == count).isEmpty()) {
            throw new IllegalArgumentException(semantic + " editor semantic requires ARRAY<NUMBER>[" + count + "]");
        }
    }
}
