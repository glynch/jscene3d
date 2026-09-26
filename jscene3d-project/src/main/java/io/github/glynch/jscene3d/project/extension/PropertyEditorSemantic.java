/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

/** Closed editor meaning attached to a structurally typed project property. */
public enum PropertyEditorSemantic {
    /** No specialized semantic beyond the property's structural shape. */
    DEFAULT("default"),
    /** Integral numeric value represented by the portable decimal number kind. */
    INTEGER("integer"),
    /** Two-axis numeric vector. */
    VECTOR2(PropertyEditorSemantics.VECTOR2),
    /** Three-axis numeric vector. */
    VECTOR3(PropertyEditorSemantics.VECTOR3),
    /** Three-axis Euler rotation authored in degrees and applied in XYZ order. */
    EULER_ROTATION(PropertyEditorSemantics.EULER_ROTATION),
    /** Four-axis normalized quaternion. */
    QUATERNION(PropertyEditorSemantics.QUATERNION),
    /** Three-channel linear-sRGB color. */
    LINEAR_COLOR(PropertyEditorSemantics.LINEAR_COLOR);

    private final String serializedName;

    /** Stores the stable extension-descriptor spelling. */
    PropertyEditorSemantic(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable extension-descriptor spelling.
     *
     * @return serialized semantic name
     */
    public String serializedName() {
        return serializedName;
    }

    /** Resolves a known spelling, retaining unknown metadata as the default semantic. */
    static PropertyEditorSemantic fromMetadata(String value) {
        for (PropertyEditorSemantic semantic : values()) {
            if (semantic.serializedName.equals(value)) {
                return semantic;
            }
        }
        return DEFAULT;
    }
}
