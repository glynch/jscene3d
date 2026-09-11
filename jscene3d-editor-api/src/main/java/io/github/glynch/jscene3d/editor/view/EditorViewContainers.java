/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** Well-known workbench containers which host contributed views. */
public final class EditorViewContainers {
    /** Left-hand workbench sidebar, currently containing the Hierarchy. */
    public static final ViewContainerId PRIMARY_SIDEBAR =
            new ViewContainerId("io.github.glynch.jscene3d.editor.primary-sidebar");

    /** Main editor area, currently containing the scene viewport. */
    public static final ViewContainerId EDITOR_AREA =
            new ViewContainerId("io.github.glynch.jscene3d.editor.editor-area");

    /** Right-hand workbench sidebar, currently containing the Inspector. */
    public static final ViewContainerId SECONDARY_SIDEBAR =
            new ViewContainerId("io.github.glynch.jscene3d.editor.secondary-sidebar");

    /** Bottom workbench panel, currently containing Project and Diagnostics views. */
    public static final ViewContainerId BOTTOM_PANEL =
            new ViewContainerId("io.github.glynch.jscene3d.editor.bottom-panel");

    private EditorViewContainers() {}
}
