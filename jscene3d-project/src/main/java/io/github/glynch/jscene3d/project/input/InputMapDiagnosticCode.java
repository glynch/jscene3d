/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for authored input maps. */
public enum InputMapDiagnosticCode implements DiagnosticCode {
    /** The input-map file is missing. */
    FILE_MISSING("input-map.file.missing", "The input-map definition does not exist or is not a regular file"),
    /** The input-map file cannot be read. */
    FILE_READ_FAILED("input-map.file.read", "The input-map definition could not be read"),
    /** The input-map path escapes the project. */
    PATH_ESCAPES_PROJECT("input-map.path.escape", "The input-map definition resolves outside the project directory"),
    /** The input-map document is invalid JSON. */
    JSON_INVALID("input-map.json", "The input-map definition is not valid JScene3D Input Map JSON"),
    /** The input-map schema version is unsupported. */
    SCHEMA_UNSUPPORTED("input-map.schema.unsupported", "The input-map schema version is unsupported"),
    /** The input-map schema URI is invalid. */
    SCHEMA_URI_INVALID("input-map.schema.uri", "The input-map schema URI is invalid"),
    /** The actions object is missing. */
    ACTIONS_REQUIRED("input-map.actions.required", "The input map requires an actions object"),
    /** The actions object is empty. */
    ACTIONS_EMPTY("input-map.actions.empty", "The input map requires at least one action"),
    /** An action identifier is invalid. */
    ACTION_ID_INVALID("input-map.action.id", "An input action identifier is invalid"),
    /** An action has no bindings. */
    BINDINGS_EMPTY("input-map.bindings.empty", "An input action requires at least one binding"),
    /** A binding entry is missing. */
    BINDING_REQUIRED("input-map.binding.required", "An input binding must be an object"),
    /** A binding device is unsupported. */
    DEVICE_UNSUPPORTED("input-map.binding.device", "The input binding device is unsupported"),
    /** A binding control is missing. */
    CONTROL_REQUIRED("input-map.binding.control", "The input binding requires a physical control"),
    /** A binding contains a property for the wrong device. */
    CONTROL_CONFLICT("input-map.binding.conflict", "The input binding contains conflicting control properties"),
    /** A physical binding is duplicated for one action. */
    BINDING_DUPLICATE("input-map.binding.duplicate", "The input action contains a duplicate physical binding");

    private final String value;
    private final String message;

    InputMapDiagnosticCode(String value, String message) {
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
