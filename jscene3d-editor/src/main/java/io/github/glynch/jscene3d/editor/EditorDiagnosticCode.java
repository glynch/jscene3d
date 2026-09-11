/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes for assembling a read-only editor session. */
enum EditorDiagnosticCode implements DiagnosticCode {
    /** An unexpected failure stopped the editor from opening a project. */
    PROJECT_LOAD_FAILED("editor.project.load", "The project could not be opened"),
    /** Project extension metadata could not be discovered. */
    EXTENSION_METADATA_UNAVAILABLE("editor.extension.metadata", "Project extension metadata could not be discovered"),
    /** Built-in and project type metadata could not be combined. */
    TYPE_CATALOG_INVALID("editor.type.catalog", "Project type metadata contains conflicting registrations"),
    /** Published imported content could not be opened. */
    IMPORT_CONTENT_UNAVAILABLE("editor.import.content", "Published imported content could not be opened"),
    /** The configured startup world is not an authored world definition. */
    STARTUP_WORLD_MISSING("editor.startup-world.missing", "The configured startup world is not in the asset catalog"),
    /** The startup world could not be realized as an editor-safe preview. */
    PREVIEW_COMPOSITION_FAILED("editor.preview.composition", "The startup-world preview could not be composed"),
    /** The startup world provides no camera through which the editor can render it. */
    PREVIEW_CAMERA_MISSING("editor.preview.camera", "The startup-world preview has no enabled primary camera");

    private final String code;
    private final String message;

    /** Stores one stable code and its locale-neutral fallback. */
    EditorDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
