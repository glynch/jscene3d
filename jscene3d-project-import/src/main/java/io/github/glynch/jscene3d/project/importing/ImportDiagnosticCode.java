/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for import execution and caching. */
public enum ImportDiagnosticCode implements DiagnosticCode {
    /** An artifact reference is invalid. */
    ARTIFACT_REFERENCE_INVALID("import.artifact.reference", "An imported artifact reference is invalid"),
    /** The import cache cannot be read. */
    CACHE_READ_FAILED("import.cache.read", "The import cache could not be read"),
    /** The import cache cannot be written. */
    CACHE_WRITE_FAILED("import.cache.write", "The import cache could not be written"),
    /** A dependency escapes the project directory. */
    DEPENDENCY_ESCAPES_PROJECT("import.dependency.escape", "An import dependency resolves outside the project"),
    /** A dependency is not a project file. */
    DEPENDENCY_INVALID("import.dependency.invalid", "An import dependency is not a project file"),
    /** A dependency cannot be read. */
    DEPENDENCY_READ_FAILED("import.dependency.read", "An import dependency could not be read"),
    /** The requested importer is missing. */
    IMPORTER_MISSING("import.importer.missing", "The requested importer is not registered"),
    /** Source inspection failed. */
    INSPECTION_FAILED("import.inspect.failed", "Source inspection failed"),
    /** Source-item settings identify a missing item. */
    ITEM_SETTINGS_MISSING("import.item-settings.missing", "Selected source-item settings are missing"),
    /** Source-item settings are outside the selected closure. */
    ITEM_SETTINGS_UNUSED("import.item-settings.unused", "Item settings do not identify a discovered source item"),
    /** Source preparation failed. */
    PREPARATION_FAILED("import.prepare.failed", "Source import preparation failed"),
    /** A selected source item is missing. */
    SELECTION_MISSING("import.selection.missing", "A selected source item was not discovered"),
    /** A selected source item cannot be selected. */
    SELECTION_NOT_SELECTABLE("import.selection.not-selectable", "A selected source item is not selectable"),
    /** A source-item reference is invalid. */
    SOURCE_ITEM_REFERENCE_INVALID("import.source-item.reference", "A source-item reference is invalid"),
    /** The source asset cannot be read. */
    SOURCE_READ_FAILED("import.source.read", "The import source asset could not be read"),
    /** The import status cannot be read. */
    STATUS_READ_FAILED("import.status.read", "The import status could not be read");

    private final String value;
    private final String message;

    ImportDiagnosticCode(String value, String message) {
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
