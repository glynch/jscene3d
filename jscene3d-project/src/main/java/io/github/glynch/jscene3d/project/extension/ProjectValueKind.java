/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** Structural kinds supported by portable project values. */
public enum ProjectValueKind {
    /** Explicit null. */
    NULL,
    /** Boolean scalar. */
    BOOLEAN,
    /** Arbitrary-precision number. */
    NUMBER,
    /** Text scalar. */
    TEXT,
    /** Ordered array. */
    ARRAY,
    /** Ordered object. */
    OBJECT,
    /** Typed project resource reference. */
    REFERENCE;

    /**
     * Returns the structural kind of a portable project value.
     *
     * @param value project value
     * @return structural value kind
     */
    public static ProjectValueKind of(ProjectValue value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case ProjectValue.NullValue ignored -> NULL;
            case ProjectValue.BooleanValue ignored -> BOOLEAN;
            case ProjectValue.NumberValue ignored -> NUMBER;
            case ProjectValue.TextValue ignored -> TEXT;
            case ProjectValue.ArrayValue ignored -> ARRAY;
            case ProjectValue.ObjectValue ignored -> OBJECT;
            case ProjectValue.ReferenceValue ignored -> REFERENCE;
        };
    }
}
