/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectValues;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

/** Converts portable project values into 3d engine-specific value types. */
final class Scene3dValues {
    /** Prevents construction of this stateless conversion policy. */
    private Scene3dValues() {
        throw new AssertionError("Scene3dValues cannot be instantiated");
    }

    /** Returns one required finite single-precision number. */
    static float number(Map<String, ProjectValue> properties, String id) {
        return ProjectValues.finiteFloat(properties, id);
    }

    /** Returns one required boolean. */
    static boolean bool(Map<String, ProjectValue> properties, String id) {
        return ProjectValues.bool(properties, id);
    }

    /** Returns one required resource reference. */
    static ResourceReference reference(Map<String, ProjectValue> properties, String id) {
        return ProjectValues.reference(properties, id);
    }

    /** Returns one required three-number array as a finite vector. */
    static Vector3f vector3(Map<String, ProjectValue> properties, String id) {
        List<ProjectValue> values = ProjectValues.array(properties, id);
        if (values.size() != 3) {
            throw new IllegalArgumentException(id + " must contain exactly three numbers");
        }
        return new Vector3f(number(values, id, 0), number(values, id, 1), number(values, id, 2));
    }

    /** Returns one required {@code #RRGGBB} sRGB color. */
    static Color color(Map<String, ProjectValue> properties, String id) {
        String value = ProjectValues.text(properties, id);
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
        return ProjectValues.finiteFloat(values.get(index), id + '[' + index + ']');
    }
}
