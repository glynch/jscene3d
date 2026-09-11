/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

/** Major workbench regions whose visibility is controlled by the session layout. */
public enum EditorWorkbenchPart {
    /** Narrow activity selector beside the primary side bar. */
    ACTIVITY_BAR,
    /** Primary side bar which normally hosts the selected activity. */
    PRIMARY_SIDEBAR,
    /** Secondary side bar which normally hosts the Inspector. */
    SECONDARY_SIDEBAR,
    /** Collapsible panel beneath the central editor area. */
    PANEL,
    /** Concise contextual strip along the bottom edge. */
    STATUS_BAR
}
