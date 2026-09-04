/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl.internal;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Vector3f;

/** Typed conversions from catalog-validated project properties to engine values. */
final class ProjectValues {
    private ProjectValues() {
        throw new AssertionError("not instantiable");
    }

    /** Returns one required finite single-precision number. */
    static float number(Map<String, ProjectValue> properties, String id) {
        ProjectValue.NumberValue number = require(properties, id, ProjectValue.NumberValue.class);
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(id + " must be representable as a finite float");
        }
        return result;
    }

    /** Returns one required boolean. */
    static boolean bool(Map<String, ProjectValue> properties, String id) {
        return require(properties, id, ProjectValue.BooleanValue.class).value();
    }

    /** Returns one required resource reference. */
    static ResourceReference reference(Map<String, ProjectValue> properties, String id) {
        return require(properties, id, ProjectValue.ReferenceValue.class).reference();
    }

    /** Returns one required three-number array as a finite vector. */
    static Vector3f vector3(Map<String, ProjectValue> properties, String id) {
        List<ProjectValue> values =
                require(properties, id, ProjectValue.ArrayValue.class).values();
        if (values.size() != 3) {
            throw new IllegalArgumentException(id + " must contain exactly three numbers");
        }
        return new Vector3f(number(values, id, 0), number(values, id, 1), number(values, id, 2));
    }

    /** Returns one required {@code #RRGGBB} sRGB color. */
    static Color color(Map<String, ProjectValue> properties, String id) {
        String value = require(properties, id, ProjectValue.TextValue.class).value();
        if (value.length() != 7 || value.charAt(0) != '#') {
            throw new IllegalArgumentException(id + " must use #RRGGBB syntax");
        }
        try {
            return Color.srgb(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(id + " must use #RRGGBB syntax", exception);
        }
    }

    /** Converts one indexed array entry to a finite float. */
    private static float number(List<ProjectValue> values, String id, int index) {
        ProjectValue value = values.get(index);
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(id + " must contain only numbers");
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(id + " must contain only finite float values");
        }
        return result;
    }

    /** Returns one required property with its expected catalog-validated representation. */
    private static <T extends ProjectValue> T require(
            Map<String, ProjectValue> properties, String id, Class<T> valueType) {
        ProjectValue value = Objects.requireNonNull(properties.get(id), id);
        if (!valueType.isInstance(value)) {
            throw new IllegalArgumentException(id + " must be a " + valueType.getSimpleName());
        }
        return valueType.cast(value);
    }
}
