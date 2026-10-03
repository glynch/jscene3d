/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.sceneview;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes produced while creating an editor-safe Scene View projection. */
public enum SceneViewDiagnosticCode implements DiagnosticCode {
    /** An internally inconsistent visual value prevented a coherent snapshot. */
    PROJECTION_FAILED("editor.scene-view.projection", "The Scene View projection could not be created");

    private final String code;
    private final String message;

    /** Stores one stable code and locale-neutral fallback. */
    SceneViewDiagnosticCode(String code, String message) {
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
