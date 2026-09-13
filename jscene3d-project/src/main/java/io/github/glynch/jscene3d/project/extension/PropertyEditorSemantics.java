/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

/** Standard editor semantics used to select specialized property controls. */
public final class PropertyEditorSemantics {
    /** Two-axis numeric vector. */
    public static final String VECTOR2 = "vector2";

    /** Three-axis numeric vector. */
    public static final String VECTOR3 = "vector3";

    /** Four-axis normalized quaternion. */
    public static final String QUATERNION = "quaternion";

    /** Linear-sRGB color channels. */
    public static final String LINEAR_COLOR = "color-linear";

    private PropertyEditorSemantics() {
        throw new AssertionError("PropertyEditorSemantics cannot be instantiated");
    }
}
