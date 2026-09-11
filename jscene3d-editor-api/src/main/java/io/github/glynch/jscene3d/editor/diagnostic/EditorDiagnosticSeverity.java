/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

/** Severity of one editor diagnostic occurrence. */
public enum EditorDiagnosticSeverity {
    /** Failed validation, compilation, loading, or execution. */
    ERROR,
    /** Actionable condition which does not prevent the current operation. */
    WARNING,
    /** Informational diagnostic which requires no corrective action. */
    INFORMATION,
    /** Low-priority suggestion or hint. */
    HINT
}
