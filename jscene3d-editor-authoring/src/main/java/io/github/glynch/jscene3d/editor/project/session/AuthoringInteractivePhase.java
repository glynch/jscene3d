/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

/** Lifecycle phase of one Java-authoritative interactive property edit. */
public enum AuthoringInteractivePhase {
    /** Reserves the target and captures its exact pre-drag authored state. */
    BEGIN,
    /** Applies one validated preview without adding history. */
    UPDATE,
    /** Adds the complete preview sequence to history as one operation. */
    COMMIT,
    /** Restores the exact pre-drag authored state without adding history. */
    CANCEL
}
