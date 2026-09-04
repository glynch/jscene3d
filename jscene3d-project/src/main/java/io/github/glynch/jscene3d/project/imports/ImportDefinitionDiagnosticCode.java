/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for authored import definitions. */
public enum ImportDefinitionDiagnosticCode implements DiagnosticCode {
    /** An import definition file is missing. */
    FILE_MISSING("import.file.missing", "The import definition does not exist or is not a regular file"),
    /** An import definition cannot be read. */
    FILE_READ_FAILED("import.file.read", "The import definition could not be read"),
    /** An import definition is invalid JSON. */
    JSON_INVALID("import.json", "The import definition is not valid JScene3D Import JSON"),
    /** An import path escapes the project directory. */
    PATH_ESCAPES_PROJECT("import.path.escape", "The import definition resolves outside the project directory"),
    /** The import schema is unsupported. */
    SCHEMA_UNSUPPORTED("import.schema.unsupported", "The import schema version is unsupported"),
    /** The import schema URI is invalid. */
    SCHEMA_URI_INVALID("import.schema.uri", "The import schema URI is invalid"),
    /** An import source asset is missing. */
    SOURCE_MISSING("import.source.missing", "The import source asset does not exist"),
    /** An import source namespace is unsupported. */
    SOURCE_NAMESPACE_INVALID("import.source.namespace", "The import source uses an unsupported namespace"),
    /** An import selection is not an array. */
    SELECTION_NOT_ARRAY("import.selection.array", "The import selection must be an array"),
    /** A selected identity is duplicated. */
    SELECTION_DUPLICATE("import.selection.duplicate", "An import selection identity is duplicated"),
    /** A selected identity is invalid. */
    SELECTION_IDENTITY_INVALID("import.selection.identity", "An import selection identity is invalid"),
    /** A selected identity is not valid text. */
    SELECTION_TEXT_INVALID("import.selection.text", "Import selection identities must be non-blank strings"),
    /** Import settings are not an object. */
    SETTINGS_NOT_OBJECT("import.settings.object", "The import settings must be an object"),
    /** An item-settings identity is invalid. */
    ITEM_SETTINGS_IDENTITY_INVALID("import.item-settings.identity", "An item-settings identity is invalid"),
    /** The item-settings index is invalid. */
    ITEM_SETTINGS_INDEX_INVALID("import.item-settings.index", "The item-settings index must be an object"),
    /** Item settings are not an object. */
    ITEM_SETTINGS_NOT_OBJECT("import.item-settings.object", "The item settings must be an object"),
    /** A required import field is missing. */
    FIELD_REQUIRED("import.field.required", "A required import value is missing"),
    /** An optional import field is blank. */
    FIELD_BLANK("import.field.blank", "An optional import value must not be blank"),
    /** An import identifier field is invalid. */
    FIELD_IDENTIFIER_INVALID("import.field.identifier", "An import value must be a portable lowercase identifier"),
    /** An import type field is invalid. */
    FIELD_TYPE_INVALID("import.field.type", "An import value must be a registered-type identifier");

    private final String value;
    private final String message;

    ImportDefinitionDiagnosticCode(String value, String message) {
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
