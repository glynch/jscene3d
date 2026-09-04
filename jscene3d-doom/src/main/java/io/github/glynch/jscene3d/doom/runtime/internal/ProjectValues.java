/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Type-safe access to structurally validated portable project values. */
final class ProjectValues {
    /** Prevents construction of this stateless decoding policy. */
    private ProjectValues() {
        throw new AssertionError("ProjectValues cannot be instantiated");
    }

    /** Returns a required named property. */
    static ProjectValue property(Map<String, ProjectValue> properties, String name, String location) {
        ProjectValue value = properties.get(name);
        if (value == null) {
            throw invalid(location + '/' + name, "is required");
        }
        return value;
    }

    /** Returns the properties carried by an object value. */
    static Map<String, ProjectValue> object(ProjectValue value, String location) {
        if (value instanceof ProjectValue.ObjectValue object) {
            return object.values();
        }
        throw invalid(location, "must be an object");
    }

    /** Returns the elements carried by an array value. */
    static List<ProjectValue> array(ProjectValue value, String location) {
        if (value instanceof ProjectValue.ArrayValue array) {
            return array.values();
        }
        throw invalid(location, "must be an array");
    }

    /** Returns an exact 32-bit integer value. */
    static int integer(ProjectValue value, String location) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw invalid(location, "must be an integer");
        }
        BigDecimal decimal = number.value();
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(location + " must be a 32-bit integer: " + decimal, exception);
        }
    }

    /** Returns a text value. */
    static String text(ProjectValue value, String location) {
        if (value instanceof ProjectValue.TextValue text) {
            return text.value();
        }
        throw invalid(location, "must be text");
    }

    /** Returns a boolean value. */
    static boolean bool(ProjectValue value, String location) {
        if (value instanceof ProjectValue.BooleanValue bool) {
            return bool.value();
        }
        throw invalid(location, "must be boolean");
    }

    /** Creates a consistent invalid-value exception. */
    private static IllegalArgumentException invalid(String location, String requirement) {
        return new IllegalArgumentException(location + ' ' + requirement);
    }
}
