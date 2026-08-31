/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf.internal;

import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorShortData;
import de.javagl.jgltf.model.ElementType;
import java.util.Objects;

/** Converts resolved JglTF accessor values into JScene3D-owned primitive arrays. */
public final class AccessorDecoder {
    private static final int GL_BYTE = 5120;
    private static final int GL_UNSIGNED_BYTE = 5121;
    private static final int GL_SHORT = 5122;
    private static final int GL_UNSIGNED_SHORT = 5123;
    private static final int GL_UNSIGNED_INT = 5125;
    private static final int GL_FLOAT = 5126;

    /** Prevents instantiation of this static conversion utility. */
    private AccessorDecoder() {
        throw new AssertionError("AccessorDecoder cannot be instantiated");
    }

    /**
     * Decodes an accessor into floats, applying glTF normalized integer conversion.
     *
     * @param accessor accessor to decode
     * @param expectedType required element type
     * @param semantic diagnostic semantic name
     * @return flat component array
     */
    public static float[] floats(AccessorModel accessor, ElementType expectedType, String semantic) {
        AccessorModel validAccessor = Objects.requireNonNull(accessor, "accessor");
        requireType(validAccessor, expectedType, semantic);
        int components = expectedType.getNumComponents();
        float[] values = new float[Math.multiplyExact(validAccessor.getCount(), components)];
        AccessorData data = validAccessor.getAccessorData();
        for (int element = 0; element < validAccessor.getCount(); element++) {
            for (int component = 0; component < components; component++) {
                values[element * components + component] = component(validAccessor, data, element, component);
            }
        }
        return values;
    }

    /**
     * Decodes a scalar unsigned index accessor into non-negative Java indices.
     *
     * @param accessor index accessor
     * @return decoded indices
     */
    public static int[] indices(AccessorModel accessor) {
        AccessorModel validAccessor = Objects.requireNonNull(accessor, "accessor");
        requireType(validAccessor, ElementType.SCALAR, "indices");
        if (validAccessor.isNormalized()) {
            throw new IllegalArgumentException("indices must not be normalized");
        }
        int componentType = validAccessor.getComponentType();
        if (componentType != GL_UNSIGNED_BYTE
                && componentType != GL_UNSIGNED_SHORT
                && componentType != GL_UNSIGNED_INT) {
            throw new IllegalArgumentException("indices use an unsupported component type: " + componentType);
        }
        AccessorData data = validAccessor.getAccessorData();
        int[] values = new int[validAccessor.getCount()];
        for (int index = 0; index < values.length; index++) {
            long value = integer(data, index, 0);
            if (value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("index exceeds Java integer range: " + value);
            }
            values[index] = (int) value;
        }
        return values;
    }

    /** Returns one accessor component as a float. */
    private static float component(AccessorModel accessor, AccessorData data, int element, int component) {
        int componentType = accessor.getComponentType();
        if (componentType == GL_FLOAT) {
            return ((AccessorFloatData) data).get(element, component);
        }
        long value = integer(data, element, component);
        if (!accessor.isNormalized()) {
            return value;
        }
        return switch (componentType) {
            case GL_BYTE -> Math.max((float) value / Byte.MAX_VALUE, -1.0f);
            case GL_UNSIGNED_BYTE -> value / 255.0f;
            case GL_SHORT -> Math.max((float) value / Short.MAX_VALUE, -1.0f);
            case GL_UNSIGNED_SHORT -> value / 65_535.0f;
            case GL_UNSIGNED_INT -> (float) (value / 4_294_967_295.0);
            default -> throw new IllegalArgumentException("unsupported accessor component type: " + componentType);
        };
    }

    /** Returns one integer component as a signed or widened unsigned value. */
    private static long integer(AccessorData data, int element, int component) {
        return switch (data) {
            case AccessorByteData bytes -> bytes.getInt(element, component);
            case AccessorShortData shorts -> shorts.getInt(element, component);
            case AccessorIntData integers -> integers.getLong(element, component);
            default -> throw new IllegalArgumentException("accessor does not contain integer data");
        };
    }

    /** Rejects an accessor whose element shape differs from the required semantic shape. */
    private static void requireType(AccessorModel accessor, ElementType expectedType, String semantic) {
        if (accessor.getElementType() != expectedType) {
            throw new IllegalArgumentException(
                    semantic + " must use " + expectedType + " elements: " + accessor.getElementType());
        }
    }
}
