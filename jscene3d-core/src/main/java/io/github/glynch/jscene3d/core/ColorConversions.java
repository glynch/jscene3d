/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static java.lang.Math.pow;

/** Color-space conversion operations shared by core color-bearing components. */
final class ColorConversions {
    private static final float SRGB_LINEAR_THRESHOLD = 0.04045f;

    private ColorConversions() {
        throw new AssertionError("ColorConversions cannot be instantiated");
    }

    static float srgbToLinear(float value) {
        if (value <= SRGB_LINEAR_THRESHOLD) {
            return value / 12.92f;
        }
        return (float) pow((value + 0.055f) / 1.055f, 2.4f);
    }
}
