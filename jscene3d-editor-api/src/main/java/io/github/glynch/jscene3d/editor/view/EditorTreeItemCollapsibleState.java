/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** Initial expansion capability and state of an editor tree item. */
public enum EditorTreeItemCollapsibleState {
    /** Item has no children and cannot be expanded. */
    NONE,
    /** Item may have children and begins collapsed. */
    COLLAPSED,
    /** Item may have children and begins expanded. */
    EXPANDED
}
