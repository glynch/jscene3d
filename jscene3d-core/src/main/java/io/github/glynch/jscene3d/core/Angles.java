/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Common single-precision angles expressed in radians. */
public final class Angles {
    /** Thirty degrees in radians. */
    public static final float PI_OVER_SIX = (float) (Math.PI / 6.0);

    /** Forty-five degrees in radians. */
    public static final float PI_OVER_FOUR = (float) (Math.PI / 4.0);

    /** Sixty degrees in radians. */
    public static final float PI_OVER_THREE = (float) (Math.PI / 3.0);

    /** Ninety degrees in radians. */
    public static final float PI_OVER_TWO = (float) (Math.PI / 2.0);

    /** One hundred eighty degrees in radians. */
    public static final float PI = (float) Math.PI;

    /** Three hundred sixty degrees in radians. */
    public static final float TWO_PI = (float) (Math.PI * 2.0);

    /** Prevents instantiation of this constants class. */
    private Angles() {
        throw new AssertionError("Angles cannot be instantiated");
    }
}
