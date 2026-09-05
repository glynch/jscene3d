/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Type-safe access to portable values supplied to project runtime extensions. */
public final class ProjectValues {
    /** Prevents construction of this stateless value-access policy. */
    private ProjectValues() {
        throw new AssertionError("ProjectValues cannot be instantiated");
    }

    /**
     * Returns a required named property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name
     * @return required portable value
     * @throws IllegalArgumentException when the property is absent
     */
    public static ProjectValue required(Map<String, ProjectValue> properties, String name) {
        return requiredAt(properties, name, name);
    }

    /**
     * Returns a required named property below a diagnostic parent location.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name
     * @param parentLocation diagnostic location containing the property
     * @return required portable value
     * @throws IllegalArgumentException when the property is absent
     */
    public static ProjectValue required(Map<String, ProjectValue> properties, String name, String parentLocation) {
        return requiredAt(properties, name, parentLocation + '/' + name);
    }

    /**
     * Returns a required text property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return text content
     * @throws IllegalArgumentException when the property is absent or has another kind
     */
    public static String text(Map<String, ProjectValue> properties, String name) {
        return text(required(properties, name), name);
    }

    /**
     * Returns text from one portable value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return text content
     * @throws IllegalArgumentException when the value is not text
     */
    public static String text(ProjectValue value, String location) {
        if (value instanceof ProjectValue.TextValue text) {
            return text.value();
        }
        throw invalid(location, "must be text");
    }

    /**
     * Returns a required exact 32-bit integer property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return exact integer value
     * @throws IllegalArgumentException when the property is absent or not an exact integer
     */
    public static int integer(Map<String, ProjectValue> properties, String name) {
        return integer(required(properties, name), name);
    }

    /**
     * Returns an exact 32-bit integer from one portable value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return exact integer value
     * @throws IllegalArgumentException when the value is not an exact 32-bit integer
     */
    public static int integer(ProjectValue value, String location) {
        BigDecimal number = number(value, location, "must be an integer");
        try {
            return number.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(location + " must be a 32-bit integer: " + number, exception);
        }
    }

    /**
     * Returns a required finite single-precision property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return finite floating-point value
     * @throws IllegalArgumentException when the property is absent, non-numeric, or non-finite
     */
    public static float finiteFloat(Map<String, ProjectValue> properties, String name) {
        return finiteFloat(required(properties, name), name);
    }

    /**
     * Returns a finite single-precision number from one portable value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return finite floating-point value
     * @throws IllegalArgumentException when the value is non-numeric or non-finite
     */
    public static float finiteFloat(ProjectValue value, String location) {
        float result = number(value, location, "must be a number").floatValue();
        if (!Float.isFinite(result)) {
            throw invalid(location, "must be representable as a finite float");
        }
        return result;
    }

    /**
     * Returns a required numeric array as finite single-precision values.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return copied finite floating-point values
     * @throws IllegalArgumentException when the property is absent, is not an array, or contains an invalid value
     */
    public static float[] finiteFloatArray(Map<String, ProjectValue> properties, String name) {
        List<ProjectValue> values = array(properties, name);
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = finiteFloat(values.get(index), indexedLocation(name, index));
        }
        return result;
    }

    /**
     * Returns a required numeric array as exact 32-bit integer values.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return copied exact integer values
     * @throws IllegalArgumentException when the property is absent, is not an array, or contains an invalid value
     */
    public static int[] integerArray(Map<String, ProjectValue> properties, String name) {
        List<ProjectValue> values = array(properties, name);
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = integer(values.get(index), indexedLocation(name, index));
        }
        return result;
    }

    /**
     * Returns a required boolean property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return boolean value
     * @throws IllegalArgumentException when the property is absent or not boolean
     */
    public static boolean bool(Map<String, ProjectValue> properties, String name) {
        return bool(required(properties, name), name);
    }

    /**
     * Returns a boolean from one portable value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return boolean value
     * @throws IllegalArgumentException when the value is not boolean
     */
    public static boolean bool(ProjectValue value, String location) {
        if (value instanceof ProjectValue.BooleanValue bool) {
            return bool.value();
        }
        throw invalid(location, "must be boolean");
    }

    /**
     * Returns a required array property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return ordered array values
     * @throws IllegalArgumentException when the property is absent or not an array
     */
    public static List<ProjectValue> array(Map<String, ProjectValue> properties, String name) {
        return array(required(properties, name), name);
    }

    /**
     * Returns the entries from one array value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return ordered array values
     * @throws IllegalArgumentException when the value is not an array
     */
    public static List<ProjectValue> array(ProjectValue value, String location) {
        if (value instanceof ProjectValue.ArrayValue array) {
            return array.values();
        }
        throw invalid(location, "must be an array");
    }

    /**
     * Returns the entries from one object value.
     *
     * @param value portable value
     * @param location diagnostic location
     * @return named object properties
     * @throws IllegalArgumentException when the value is not an object
     */
    public static Map<String, ProjectValue> object(ProjectValue value, String location) {
        if (value instanceof ProjectValue.ObjectValue object) {
            return object.values();
        }
        throw invalid(location, "must be an object");
    }

    /**
     * Returns a required resource-reference property.
     *
     * @param properties effective properties supplied to a runtime factory
     * @param name property name and diagnostic location
     * @return resource reference
     * @throws IllegalArgumentException when the property is absent or not a reference
     */
    public static ResourceReference reference(Map<String, ProjectValue> properties, String name) {
        ProjectValue value = required(properties, name);
        if (value instanceof ProjectValue.ReferenceValue reference) {
            return reference.reference();
        }
        throw invalid(name, "must be a resource reference");
    }

    /** Requires a non-null property map and returns one value at its complete location. */
    private static ProjectValue requiredAt(Map<String, ProjectValue> properties, String name, String location) {
        ProjectValue value = Objects.requireNonNull(properties, "properties").get(name);
        if (value == null) {
            throw invalid(location, "is required");
        }
        return value;
    }

    /** Returns the arbitrary-precision content of one numeric value. */
    private static BigDecimal number(ProjectValue value, String location, String requirement) {
        if (value instanceof ProjectValue.NumberValue number) {
            return number.value();
        }
        throw invalid(location, requirement);
    }

    /** Creates one consistently formatted invalid-value exception. */
    private static IllegalArgumentException invalid(String location, String requirement) {
        return new IllegalArgumentException(location + ' ' + requirement);
    }

    /** Creates the diagnostic location of one indexed array entry. */
    private static String indexedLocation(String location, int index) {
        return location + '[' + index + ']';
    }
}
