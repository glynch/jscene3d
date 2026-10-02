/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes for runtime-free composition planning. */
public enum CompositionDiagnosticCode implements DiagnosticCode {
    /** Safe planning failed unexpectedly after validation. */
    PLANNING_FAILED("composition.planning", "Safe composition planning failed"),
    /** A required descriptor is absent after validation. */
    TYPE_MISSING("composition.type.missing", "A component descriptor is absent during composition planning"),
    /** A reusable-definition argument is invalid. */
    ARGUMENT_INVALID("composition.argument", "A reusable-definition argument is invalid"),
    /** A prepared definition graph does not contain a referenced definition. */
    DEFINITION_MISSING("composition.definition.missing", "A prepared definition graph is incomplete");

    private final String value;
    private final String message;

    CompositionDiagnosticCode(String value, String message) {
        this.value = value;
        this.message = message;
    }

    @Override
    public String code() {
        return value;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
